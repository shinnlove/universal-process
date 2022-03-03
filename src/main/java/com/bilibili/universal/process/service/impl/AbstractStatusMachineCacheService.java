/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import static com.bilibili.universal.process.consts.MachineConstant.DEFAULT_ACTION_ID;
import static com.bilibili.universal.process.consts.MachineConstant.DEFAULT_STATUS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.bilibili.universal.process.model.cache.TemplateMetadata;
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.enums.TemplateTriggerType;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.util.common.AssertUtil;

/**
 * @author Tony Zhao
 * @version $Id: AbstractStatusMachineCacheService.java, v 0.1 2022-03-02 9:23 PM Tony Zhao Exp $$
 */
public abstract class AbstractStatusMachineCacheService extends AbstractStatusMachineService {

    protected TemplateCache getCache(int templateId) {
        TemplateCache template = processMetadataService.getTemplateById(templateId);
        AssertUtil.isNotNull(template);
        return template;
    }

    protected TemplateCache getTpl(int actionId) {
        TemplateCache template = processMetadataService.getTemplateByActionId(actionId);
        AssertUtil.isNotNull(template);
        return template;
    }

    protected int defaultDst(int templateId) {
        return processMetadataService.getDstByTemplateId(templateId);
    }

    protected List<ActionHandler> triggers(int actionId) {
        TemplateCache template = getTpl(actionId);
        Map<String, List<ActionHandler>> triggers = template.getTriggers();
        Map<Integer, ActionCache> actionMap = template.getActions();
        ActionCache cache = actionMap.get(actionId);

        StatusCache[] arr = template.getStatusArray();

        return getTriggers(arr, cache.getDestination(), triggers);
    }

    protected List<ActionHandler> triggers(int templateId, int status) {
        TemplateCache template = getCache(templateId);
        Map<String, List<ActionHandler>> triggers = template.getTriggers();
        StatusCache[] arr = template.getStatusArray();

        return getTriggers(arr, status, triggers);
    }

    private List<ActionHandler> getTriggers(StatusCache[] arr, int status,
                                            Map<String, List<ActionHandler>> triggers) {
        for (int i = 0; i < arr.length; i++) {
            StatusCache sc = arr[i];
            if (sc.getNo() == status) {
                int type = sc.getAccomplish();
                if (type > 0) {
                    String typeName = TemplateTriggerType.getNameByCode(type);
                    if (typeName != null && triggers.containsKey(typeName)) {
                        return triggers.get(typeName);
                    }
                }
                break;
            }
        }

        return new ArrayList<>();
    }

    protected ActionCache getAction(int actionId) {
        TemplateCache template = getTpl(actionId);
        Map<Integer, ActionCache> actionCacheMap = template.getActions();
        return actionCacheMap.get(actionId);
    }

    protected ActionCache getAction(int templateId, int source, int destination) {
        TemplateCache template = getCache(templateId);
        Map<Integer, Map<Integer, ActionCache>> actionTable = template.getActionTable();

        if (CollectionUtils.isEmpty(actionTable) || !actionTable.containsKey(destination)) {
            return null;
        }

        Map<Integer, ActionCache> actionCacheMap = actionTable.get(destination);

        if (CollectionUtils.isEmpty(actionCacheMap)) {
            return null;
        }

        if (actionCacheMap.containsKey(source)) {
            return actionCacheMap.get(source);
        } else {
            if (actionCacheMap.containsKey(DEFAULT_STATUS)) {
                return actionCacheMap.get(DEFAULT_STATUS);
            }
        }

        return null;
    }

    protected int getActionId(int templateId, int source, int destination) {
        ActionCache cache = getAction(templateId, source, destination);
        if (Objects.nonNull(cache)) {
            return cache.getActionId();
        }

        return DEFAULT_ACTION_ID;
    }

    protected List<ActionHandler> handlers(int actionId, boolean isSync) {
        return processMetadataService.getExecutions(actionId, isSync);
    }

    protected int childTplId2Parent(int childTemplateId){
        TemplateCache cache = getCache(childTemplateId);
        TemplateMetadata metadata = cache.getMetadata();
        return metadata.getParentId();
    }

}