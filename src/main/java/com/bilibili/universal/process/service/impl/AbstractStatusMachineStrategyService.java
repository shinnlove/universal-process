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
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.cache.TemplateMetadata;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.model.status.BriefProcess;
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
public abstract class AbstractStatusMachineStrategyService extends
                                                           AbstractStatusMachineParamService {

    protected List<StatusCache> statusP2C(int parentTemplateId, int childTemplateId,
                                          int parentStatus) {
        StatusRefMapping refMapping = statusRefMapping(parentTemplateId, childTemplateId);
        if (refMapping == null) {
            return Collections.emptyList();
        }

        Map<Integer, List<StatusCache>> p2cMapping = refMapping.getParent2Child();
        if (CollectionUtils.isEmpty(p2cMapping) || !p2cMapping.containsKey(parentStatus)) {
            return Collections.emptyList();
        }

        return p2cMapping.get(parentStatus);
    }

    protected int statusC2P(int parentTemplateId, int childTemplateId, int childStatus) {
        StatusRefMapping refMapping = statusRefMapping(parentTemplateId, childTemplateId);
        if (refMapping == null) {
            return DEFAULT_STATUS;
        }

        Map<Integer, Integer> c2p = refMapping.getChild2parent();
        if (c2p.containsKey(childStatus)) {
            return c2p.get(childStatus);
        }

        return DEFAULT_STATUS;
    }

    private int getParentTplId(UniversalProcess process) {
        int id = process.getTemplateId();
        TemplateCache cache = getCache(id);
        TemplateMetadata metadata = cache.getMetadata();
        return metadata.getParentId();
    }

    /**
     * Special warning: when call this method, child tx has been submitted.
     * should not call this method when tx is not committed, 
     * if need deduce all children' will proceed status, pls use deduce slowest child instead of this!
     * 
     * @param children 
     * @return
     */
    protected BriefProcess slowestChildrenStatus(List<UniversalProcess> children) {
        BriefProcess brief = new BriefProcess(-1, -1, -1);

        if (CollectionUtils.isEmpty(children)) {
            return brief;
        }

        // all children process change to parent ref status to compare latest
        int pid = -1;
        List<Integer> parentStatus = new ArrayList<>();
        for (UniversalProcess c : children) {
            int cs = c.getCurrentStatus();
            int cid = c.getTemplateId();
            pid = getParentTplId(c);
            parentStatus.add(statusC2P(pid, cid, cs));
        }

        // parentStatus list could not be empty here
        int pStatus = Collections.min(parentStatus);

        TemplateCache cache = getCache(pid);
        StatusCache[] arr = cache.getStatusArray();

        return new BriefProcess(pid, getStatusSequence(arr, pStatus), pStatus);
    }

    protected int nearestAction(int parentTemplateId, int parentDestination, int childTemplateId,
                                int childStatus) {
        TemplateCache parent = getCache(parentTemplateId);
        // it's already initialized in order
        StatusCache[] arr = parent.getStatusArray();
        int startSeq = getStatusSequence(arr, parentDestination);

        for (int i = 0; i < arr.length; i++) {
            if (startSeq <= arr[i].getSequence()) {
                int aid = searchNearestActionOnce(parentTemplateId, arr[i].getNo(), childTemplateId,
                    childStatus);
                if (aid > 0) {
                    return aid;
                }
            }
        }

        // till end still no match nodes, we forgive proceed..
        return -1;
    }

    private int searchNearestActionOnce(int parentTemplateId, int parentDestination,
                                        int childTemplateId, int childStatus) {
        // do search nearest
        // VIP2: no need to proceed this process since child has no refer status to parent
        List<StatusCache> childrenDst = statusP2C(parentTemplateId, childTemplateId,
            parentDestination);
        if (CollectionUtils.isEmpty(childrenDst)) {
            return DEFAULT_ACTION_ID;
        }

        // keep in order again
        Collections.sort(childrenDst);

        for (StatusCache sc : childrenDst) {
            int dst = sc.getNo();

            // VIP3: remember, no need to judge back and forth, since biz status could turn around.. ×3
            // VIP4: current child status node to this refer status node has no route path! ×2
            if (dst <= 0) {
                continue;
            }

            int aid = getActionId(childTemplateId, childStatus, dst);
            if (aid < 0) {
                continue;
            }

            // only need the nearest status node.
            return aid;
        }

        return DEFAULT_ACTION_ID;
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
        int c2pRefStatus = statusC2P(parentTemplateId, childTemplateId, childStatus);
        if (c2pRefStatus < 0) {
            return false;
        }

        // judge whether current status is behind current parent ref minimum status
        return behind(parentTemplateId, c2pRefStatus, parentStatus);
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

    protected int getStatusSequence(int templateId, int status) {
        TemplateCache cache = getCache(templateId);
        StatusCache[] arr = cache.getStatusArray();
        return getStatusSequence(arr, status);
    }

    protected int getStatusSequence(StatusCache[] arr, int status) {
        for (int i = 0; i < arr.length; i++) {
            if (status == arr[i].getNo()) {
                return arr[i].getSequence();
            }
        }
        return -1;
    }

    protected int nextSequenceStatus(StatusCache[] arr, int seq, boolean contains) {
        for (int i = 0; i < arr.length; i++) {

            if (contains) {
                if (seq <= arr[i].getSequence()) {
                    return arr[i].getNo();
                }
            } else {
                if (seq < arr[i].getSequence()) {
                    return arr[i].getNo();
                }
            }

        }

        return -1;
    }

}