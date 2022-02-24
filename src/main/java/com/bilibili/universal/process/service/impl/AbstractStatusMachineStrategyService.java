/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.cache.TemplateMetadata;
import com.bilibili.universal.process.model.cascade.PrepareParent;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.model.status.StatusRefMapping;

/**
 * <p>
 * An abstract strategy service for cascade proceed for parent and child,
 * smart select appropriate action and input parameter for child action.
 * </p>
 * 
 * @author Tony Zhao
 * @version $Id: AbstractStatusMachineStrategyService.java, v 0.1 2022-02-23 4:50 PM Tony Zhao Exp $$
 */
public abstract class AbstractStatusMachineStrategyService extends AbstractStatusMachineService {

    protected PrepareParent getPrepare(Map<String, Integer> prepare) {
        for (Map.Entry<String, Integer> entry : prepare.entrySet()) {
            String prepareName = entry.getKey();
            int prepareId = entry.getValue();
            return new PrepareParent(prepareName, prepareId);
        }
        return null;
    }

    protected Object getResultByClass(ProcessContext context, String className) {
        Object result = null;
        Map<String, Object> results = context.getResultObject();
        if (!CollectionUtils.isEmpty(results) && results.containsKey(className)) {
            result = results.get(className);
        }
        return result;
    }

    private Object lastHandlerResult(ActionCache cache, ProcessContext context) {
        // use default if not exist
        List<ActionHandler> syncHandlers = cache.getSyncHandlers();
        int size = syncHandlers.size();
        ActionHandler h = syncHandlers.get(size - 1);
        String name = h.getClass().getName();
        return getResultByClass(context, name);
    }

    protected int chooseParentAction(int parentTemplateId, int source, int destination) {
        int actionId = -1;
        TemplateCache cache = getCache(parentTemplateId);

        // no target action id
        Map<Integer, Map<Integer, ActionCache>> parentActionDst = cache.getActionTable();
        if (CollectionUtils.isEmpty(parentActionDst) || !parentActionDst.containsKey(destination)) {
            return actionId;
        }

        Map<Integer, ActionCache> dstMapping = parentActionDst.get(destination);
        if (CollectionUtils.isEmpty(dstMapping) || !dstMapping.containsKey(source)) {
            return actionId;
        }

        // target parent action id
        ActionCache dstAction = dstMapping.get(source);
        return dstAction.getActionId();
    }

    protected Object chooseParentParam(ActionCache cache, ProcessContext context,
                                       int parentTemplateId) {
        PrepareParent prepare = getPrepare(cache.getPrepareHandler());
        if (prepare != null && prepare.getParentTemplateId() == parentTemplateId) {
            // has prepare parameter
            return getResultByClass(context, prepare.getClassName());
        } else {
            return lastHandlerResult(cache, context);
        }
    }

    protected int slowestChildrenStatus(List<UniversalProcess> children) {
        int min = -1;

        if (CollectionUtils.isEmpty(children)) {
            return min;
        }

        UniversalProcess process = children.get(0);
        int id = process.getTemplateId();
        TemplateCache cache = getCache(id);
        StatusCache[] arr = cache.getStatusArray();

        if (arr.length <= 0) {
            return min;
        }

        min = getStatusSequence(arr, process.getCurrentStatus());

        for (UniversalProcess c : children) {
            int cs = c.getCurrentStatus();
            int seq = getStatusSequence(arr, cs);
            if (seq < min) {
                min = seq;
            }
        }

        return getStatusNo(arr, min);
    }

    protected int nearestChildAction(int childTemplateId, int childStatus, int parentSource,
                                     int parentDestination) {
        int actionId = -1;

        TemplateCache cache = getCache(childTemplateId);
        Map<Integer, Map<Integer, ActionCache>> childActionDst = cache.getActionTable();

        // no target action id
        if (CollectionUtils.isEmpty(childActionDst)) {
            return actionId;
        }

        TemplateMetadata metadata = cache.getMetadata();
        int parentTemplateId = metadata.getParentId();

        StatusRefMapping refMapping = statusRefMapping(parentTemplateId, childTemplateId);
        if (refMapping == null) {
            return actionId;
        }

        Map<Integer, List<StatusCache>> p2cMapping = refMapping.getParent2Child();
        if (!p2cMapping.containsKey(parentDestination)) {
            return actionId;
        }

        int targetDst = -1;
        List<StatusCache> childrenDst = p2cMapping.get(parentDestination);
        if (!CollectionUtils.isEmpty(childrenDst)) {
            StatusCache sc = childrenDst.get(0);
            targetDst = sc.getNo();
        }

        // not found or no mapping action..
        if (targetDst <= 0 || !childActionDst.containsKey(targetDst)
            || !childActionDst.get(targetDst).containsKey(childStatus)) {
            return actionId;
        }

        Map<Integer, ActionCache> targets = childActionDst.get(targetDst);
        ActionCache targetAction = targets.get(childStatus);

        return targetAction.getActionId();
    }

    protected Object chooseChildParam(ActionCache cache, ProcessContext context) {
        // parent no need to prepare for children since it has multiple children
        // just fetch last handler's result for child process
        return lastHandlerResult(cache, context);
    }

    protected boolean inParentRefStatus(int parentTemplateId, int parentStatus, int childTemplateId,
                                        int childStatus) {
        StatusRefMapping refMapping = statusRefMapping(parentTemplateId, childTemplateId);
        Map<Integer, List<StatusCache>> childrenRef = refMapping.getParent2Child();
        List<StatusCache> childrenStatus = childrenRef.get(parentStatus);

        for (StatusCache sc : childrenStatus) {
            if (sc.getNo() == childStatus) {
                return true;
            }
        }

        return false;
    }

    protected int getStatusSequence(StatusCache[] arr, int status) {
        for (int i = 0; i < arr.length; i++) {
            if (status == arr[i].getNo()) {
                return arr[i].getSequence();
            }
        }
        return -1;
    }

    protected int getStatusNo(StatusCache[] arr, int sequence) {
        for (int i = 0; i < arr.length; i++) {
            if (sequence == arr[i].getSequence()) {
                return arr[i].getNo();
            }
        }
        return -1;
    }

    protected int statusC2P(int parentTemplateId, int childTemplateId, int childStatus) {
        int mappingStatus = -1;

        StatusRefMapping refMapping = statusRefMapping(parentTemplateId, childTemplateId);
        if (refMapping == null) {
            return mappingStatus;
        }

        Map<Integer, Integer> c2p = refMapping.getChild2parent();
        if (c2p.containsKey(childStatus)) {
            return c2p.get(childStatus);
        }

        return mappingStatus;
    }

    protected boolean behind(int templateId, int former, int latter) {
        TemplateCache cache = getCache(templateId);
        StatusCache[] arr = cache.getStatusArray();

        int formerSeq = getStatusSequence(arr, former);
        int latterSeq = getStatusSequence(arr, latter);

        // behind means smaller than sequence
        if (formerSeq < latterSeq) {
            return true;
        }

        return false;
    }

}