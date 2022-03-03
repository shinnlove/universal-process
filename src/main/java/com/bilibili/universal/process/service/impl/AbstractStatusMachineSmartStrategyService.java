/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import static com.bilibili.universal.process.consts.MachineConstant.DEFAULT_ACTION_ID;
import static com.bilibili.universal.process.consts.MachineConstant.DEFAULT_STATUS;

import java.util.Objects;

import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * @author Tony Zhao
 * @version $Id: AbstractStatusMachineSmartStrategyService.java, v 0.1 2022-03-02 12:08 PM Tony Zhao Exp $$
 */
public abstract class AbstractStatusMachineSmartStrategyService extends
                                                                AbstractStatusMachineStrategyService {

    protected int nextActionDst(int templateId, int current, int min, boolean contains) {
        TemplateCache template = getCache(templateId);
        StatusCache[] arr = template.getStatusArray();

        int minSeq = getStatusSequence(arr, min);
        int statusNo = min;

        while (statusNo > 0) {
            statusNo = nextSequenceStatus(arr, minSeq, contains);

            ActionCache cache = getAction(templateId, current, statusNo);
            if (Objects.nonNull(cache)) {
                return cache.getDestination();
            }

            minSeq += 1;
        }

        return DEFAULT_ACTION_ID;
    }

    protected ProcessContext reject(int templateId, long refUniqueNo, DataContext dataContext) {
        return buildContext(templateId, DEFAULT_ACTION_ID, refUniqueNo, DEFAULT_STATUS,
            DEFAULT_STATUS, dataContext);
    }

}