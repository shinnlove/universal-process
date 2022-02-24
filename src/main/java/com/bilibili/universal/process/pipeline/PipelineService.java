/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.pipeline;

import java.util.function.Consumer;

import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * @author Tony Zhao
 * @version $Id: PipelineService.java, v 0.1 2021-12-28 11:19 PM Tony Zhao Exp $$
 */
public interface PipelineService {

    /**
     * @param actionId    
     * @param dataContext
     * @return
     */
    Object doPipeline(final int actionId, final DataContext dataContext);

    /**
     * @param actionId 
     * @param dataContext
     * @param callback
     * @return
     */
    Object doPipeline(final int actionId, final DataContext dataContext,
                      final Consumer<ProcessContext> callback);

}
