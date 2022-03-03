/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import static com.bilibili.universal.process.consts.MachineConstant.DEFAULT_ACTION_ID;
import static com.bilibili.universal.process.consts.MachineConstant.DEFAULT_STATUS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.model.status.BriefProcess;

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

    protected List<BriefProcess> deduceParentProceed(int parentTemplateId, int parentSrc,
                                                     int parentDeduceDst,
                                                     List<UniversalProcess> refChildren) {
        List<BriefProcess> children = new ArrayList<>();

        for (UniversalProcess c : refChildren) {
            int cTid = c.getTemplateId();
            int cStatus = c.getCurrentStatus();
            int cDst = cStatus;

            // add filter logic in ref child which status is not in refer status!
            // in another words: parent's source status is beyond child status already
            boolean involved = false;
            List<StatusCache> refP2C = statusP2C(parentTemplateId, cTid, parentSrc);
            for (StatusCache sc : refP2C) {
                if (cStatus == sc.getNo()) {
                    involved = true;
                    break;
                }
            }

            if (involved) {
                // search nearest action for proceed.
                int aid = nearestAction(parentTemplateId, parentDeduceDst, cTid, cStatus);
                if (aid > 0) {
                    ActionCache action = getAction(aid);
                    cDst = action.getDestination();
                }
            }

            children.add(new BriefProcess(cTid, cDst, getStatusSequence(cTid, cDst)));
        }

        return children;
    }

    protected int slowestC2PStatus(int parentTemplateId, List<BriefProcess> children) {

        if (CollectionUtils.isEmpty(children)) {
            return DEFAULT_STATUS;
        }

        Collections.sort(children);

        int min = Integer.MAX_VALUE;
        for (BriefProcess c : children) {
            int cTid = c.getTid();

            int pStatus = statusC2P(parentTemplateId, cTid, c.getStatus());
            if (pStatus < min) {
                min = pStatus;
            }
        }

        return min;
    }

}