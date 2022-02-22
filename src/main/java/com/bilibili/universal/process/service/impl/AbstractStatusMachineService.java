/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cascade.PrepareParent;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.service.StatusMachine2ndService;
import com.bilibili.universal.util.code.SystemResultCode;
import com.bilibili.universal.util.exception.SystemException;

/**
 * @author Tony Zhao
 * @version $Id: AbstractStatusMachineService.java, v 0.1 2022-02-22 2:19 PM Tony Zhao Exp $$
 */
public abstract class AbstractStatusMachineService implements StatusMachine2ndService {

    protected Object chooseParam(ActionCache cache, ProcessContext context, int parentTemplateId) {
        PrepareParent prepare = getPrepare(cache.getPrepareHandler());
        if (prepare != null && prepare.getParentTemplateId() == parentTemplateId) {
            // has prepare parameter
            return getResultByClass(context, prepare.getClassName());
        } else {
            // use default if not exist
            List<ActionHandler> syncHandlers = cache.getSyncHandlers();
            int size = syncHandlers.size();
            ActionHandler h = syncHandlers.get(size - 1);
            String name = h.getClass().getName();
            return getResultByClass(context, name);
        }
    }

    protected Object getResultByClass(ProcessContext context, String className) {
        Object result = null;
        Map<String, Object> results = context.getResultObject();
        if (!CollectionUtils.isEmpty(results) && results.containsKey(className)) {
            result = results.get(className);
        }
        return result;
    }

    protected PrepareParent getPrepare(Map<String, Integer> prepare) {
        for (Map.Entry<String, Integer> entry : prepare.entrySet()) {
            String prepareName = entry.getKey();
            int prepareId = entry.getValue();
            return new PrepareParent(prepareName, prepareId);
        }
        return null;
    }

    protected ProcessContext buildProContext(int templateId, long refUniqueNo, int source,
                                             int destination, DataContext dataContext) {
        ProcessContext context = new ProcessContext();
        context.setTemplateId(templateId);
        context.setRefUniqueNo(refUniqueNo);
        context.setSourceStatus(source);
        context.setDestinationStatus(destination);
        context.setDataContext(dataContext);
        return context;
    }

    protected ProcessContext buildProContext(int templateId, int actionId, long refUniqueNo,
                                             int source, int destination, DataContext dataContext) {
        ProcessContext context = new ProcessContext();
        context.setTemplateId(templateId);
        context.setActionId(actionId);
        context.setRefUniqueNo(refUniqueNo);
        context.setSourceStatus(source);
        context.setDestinationStatus(destination);
        context.setDataContext(dataContext);
        return context;
    }

    protected void checkSourceStatus(int currentStatus, int source) {
        if (currentStatus != source) {
            throw new SystemException(SystemResultCode.PARAM_INVALID,
                MachineConstant.SOURCE_STATUS_INCORRECT);
        }
    }

}