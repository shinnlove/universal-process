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
import com.bilibili.universal.process.model.cascade.PrepareParent;
import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * @author Tony Zhao
 * @version $Id: AbstractStatusMachineParamService.java, v 0.1 2022-03-02 8:26 PM Tony Zhao Exp $$
 */
public abstract class AbstractStatusMachineParamService extends AbstractStatusMachineService {

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

    protected Object lastHandlerResult(ActionCache cache, ProcessContext context) {
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

    protected Object chooseChildParam(ActionCache cache, ProcessContext context) {
        // parent no need to prepare for children since it has multiple children
        // just fetch last handler's result for child process
        return lastHandlerResult(cache, context);
    }

}