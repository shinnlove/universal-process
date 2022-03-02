/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.Objects;

import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;

/**
 * @author Tony Zhao
 * @version $Id: AbstractStatusMachineSmartStrategyService.java, v 0.1 2022-03-02 12:08 PM Tony Zhao Exp $$
 */
public abstract class AbstractStatusMachineSmartStrategyService extends
                                                                AbstractStatusMachineStrategyService {

    protected int nextActionDst(int templateId, int current, boolean contains) {
        TemplateCache template = getCache(templateId);
        StatusCache[] arr = template.getStatusArray();

        int currentSeq = getStatusSequence(arr, current);
        int statusNo = current;

        while (statusNo > 0) {
            statusNo = nextSequenceStatus(arr, currentSeq, contains);

            ActionCache cache = getAction(templateId, current, statusNo);
            if (Objects.nonNull(cache)) {
                return cache.getDestination();
            }

            currentSeq += 1;
        }

        return -1;
    }

}