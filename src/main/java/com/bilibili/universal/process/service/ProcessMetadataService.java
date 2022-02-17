/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service;

import java.util.List;

import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.cache.TemplateCache;

/**
 * @author Tony Zhao
 * @version $Id: ProcessMetadataService.java, v 0.1 2022-02-09 3:59 PM Tony Zhao Exp $$
 */
public interface ProcessMetadataService {

    /**
     * @param templateId 
     * @return
     */
    TemplateCache getTemplateById(int templateId);

    /**
     * @param actionId 
     * @return
     */
    TemplateCache getTemplateByActionId(int actionId);

    /**
     * Get specific handlers by action id.
     * 
     * @param actionId 
     * @param sync          sync handlers or async handlers
     * @return
     */
    @SuppressWarnings("rawtypes")
    List<ActionHandler> getExecutions(int actionId, boolean sync);

    boolean isFinalStatus(int templateId, int status);

    int getACStatus(int templateId);

}
