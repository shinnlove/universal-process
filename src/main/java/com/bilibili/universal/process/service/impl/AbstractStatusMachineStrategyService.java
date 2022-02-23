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
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.cascade.PrepareParent;
import com.bilibili.universal.process.model.context.ProcessContext;

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
        // todo: here 
        TemplateCache cache = getCache(parentTemplateId);
        Map<Integer, Map<Integer, ActionCache>> parentActionDst = cache.getActionTable();
        if (parentActionDst.containsKey(destination)) {
            Map<Integer, ActionCache> dstMapping = parentActionDst.get(destination);
            if (dstMapping.containsKey(source)) {
                ActionCache dstAction = dstMapping.get(source);
                // parent revising action id
                return dstAction.getActionId();
            }
        }
        return -1;
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

    protected int chooseChildAction(int childTemplateId, int source, int destination) {
        TemplateCache cache = getCache(childTemplateId);
        Map<Integer, Map<Integer, ActionCache>> childActionDst = cache.getActionTable();

        return -1;
    }

    protected Object chooseChildParam(ActionCache cache, ProcessContext context) {
        // parent no need to prepare for children since it has multiple children
        // just fetch last handler's result for child process
        return lastHandlerResult(cache, context);
    }

}