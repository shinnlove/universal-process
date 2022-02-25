/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
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
        if (CollectionUtils.isEmpty(syncHandlers)) {
            return null;
        }

        int size = syncHandlers.size();
        ActionHandler h = syncHandlers.get(size - 1);
        String name = h.getClass().getName();
        return getResultByClass(context, name);
    }

    protected int appropriateAction(int parentTemplateId, int source, int destination) {
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

    protected int nearestAction(int parentTemplateId, int parentDestination, int childTemplateId,
                                int childStatus) {
        TemplateCache parent = getCache(parentTemplateId);
        // it's initialized in order
        StatusCache[] arr = parent.getStatusArray();
        int startSeq = getStatusSequence(arr, parentDestination);

        for (int i = 0; i < arr.length; i++) {
            if (startSeq <= arr[i].getSequence()) {
                int aid = searchNearestOnce(parentTemplateId, arr[i].getNo(), childTemplateId,
                    childStatus);
                if (aid > 0) {
                    return aid;
                }
            }
        }

        // till end still no match nodes, we forgive proceed..
        return -1;
    }

    protected int searchNearestOnce(int parentTemplateId, int parentDestination,
                                    int childTemplateId, int childStatus) {
        int actionId = -1;

        TemplateCache template = getCache(childTemplateId);
        Map<Integer, Map<Integer, ActionCache>> childDstRoute = template.getActionTable();
        if (CollectionUtils.isEmpty(childDstRoute)) {
            // template has no action is a wrong case!
            // VIP1: no need to proceed this process since child has no route to dst 
            return actionId;
        }

        StatusRefMapping refMapping = statusRefMapping(parentTemplateId, childTemplateId);
        if (refMapping == null) {
            return actionId;
        }

        Map<Integer, List<StatusCache>> p2cMapping = refMapping.getParent2Child();
        if (CollectionUtils.isEmpty(p2cMapping) || !p2cMapping.containsKey(parentDestination)) {
            return actionId;
        }

        // do search nearest
        // VIP2: no need to proceed this process since child has no refer status to parent

        // keep in order again
        List<StatusCache> childrenDst = p2cMapping.get(parentDestination);
        Collections.sort(childrenDst);

        for (StatusCache sc : childrenDst) {
            int eachDst = sc.getNo();

            // VIP3: remember, no need to judge back and forth, since biz status could turn around.. ×3
            // ...

            if (eachDst <= 0 || !childDstRoute.containsKey(eachDst)
                || !childDstRoute.get(eachDst).containsKey(childStatus)) {
                // VIP4: current child status node to this refer status node has no route path! ×2
                continue;
            }

            // targeting one route path..
            Map<Integer, ActionCache> actionMap = childDstRoute.get(eachDst);
            ActionCache action = actionMap.get(childStatus);

            actionId = action.getActionId();

            // only need the nearest status node.
            break;
        }

        return actionId;
    }

    protected Object chooseChildParam(ActionCache cache, ProcessContext context) {
        // parent no need to prepare for children since it has multiple children
        // just fetch last handler's result for child process
        return lastHandlerResult(cache, context);
    }

    /**
     * Pickup the minimum status in child status list which reference by dst parent status.
     * This child should be proceed if current child status is behind the minimum status.
     * 
     * @param parentTemplateId      the parent template id
     * @param parentStatus          current proceed parent dst status
     * @param childTemplateId       the child template id
     * @param childStatus           current status of child process
     * @return
     */
    protected boolean behindRefStatus(int parentTemplateId, int parentStatus, int childTemplateId,
                                      int childStatus) {
        StatusRefMapping refMapping = statusRefMapping(parentTemplateId, childTemplateId);
        if (refMapping == null) {
            return false;
        }

        Map<Integer, Integer> parentRef = refMapping.getChild2parent();
        if (CollectionUtils.isEmpty(parentRef)) {
            return false;
        }

        int childRefInParentStatus = parentRef.get(childStatus);

        // judge whether current status is behind current parent ref minimum status
        return behind(parentTemplateId, childRefInParentStatus, parentStatus);
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

    @Deprecated
    private void reconcileParent(long selfProcessNo, long parentRefUniqueNo, int needReconcile,
                                 int reconcileMode) {
        // if status reached to end, then check reconcile mode
        if (parentRefUniqueNo != -1 && needReconcile == 1) {
            // get parent process
            UniversalProcess parentProcess = lockNoProcess(parentRefUniqueNo);
            int pTemplateId = parentProcess.getTemplateId();
            long parentProcessNo = parentProcess.getProcessNo();
            int pActionId = -1;
            int pcStatus = parentProcess.getCurrentStatus();

            // check reconcile flag
            boolean needUpdateParent = true;
            if (reconcileMode == 1) {
                // cooperate mode: get other sibling process
                // loop to check each child process status with no lock and update parent status if ok
                for (UniversalProcess up : refSiblings(parentRefUniqueNo, selfProcessNo)) {
                    int uptId = up.getTemplateId();
                    int upStatus = up.getCurrentStatus();
                    boolean isFinal = isFinalStatus(uptId, upStatus);
                    if (!isFinal) {
                        needUpdateParent = false;
                    }
                }
            }

            // need reconcile parent
            if (needUpdateParent) {
                int pacStatus = getACStatus(pTemplateId);
                proceedProcessStatus(pTemplateId, pActionId, parentProcessNo, pcStatus, pacStatus,
                    MachineConstant.DEFAULT_OPERATOR, MachineConstant.DEFAULT_REMARK);
            }

        } // if need reconcile
    }

}