/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.pipeline;

import java.util.function.Consumer;

import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * Pipeline handler service, distinguish from status machine 2nd service.
 * 
 * @author Tony Zhao
 * @version $Id: PipelineService.java, v 0.1 2021-12-28 11:19 PM Tony Zhao Exp $$
 */
public interface PipelineService {

    /**
     * Execute handlers in pipeline without status.
     * 
     * @param actionId    
     * @param dataContext
     * @return
     */
    Object doPipeline(final int actionId, final DataContext dataContext);

    /**
     * Execute handlers in pipeline with callback.
     * 
     * @param actionId 
     * @param dataContext
     * @param callback
     * @return
     */
    Object doPipeline(final int actionId, final DataContext dataContext,
                      final Consumer<ProcessContext> callback);

    /**
     * Execute handlers in pipeline using transaction.
     * 
     * @param actionId 
     * @param dataContext
     * @return
     */
    Object txPipeline(final int actionId, final DataContext dataContext);

    /**
     * Execute handlers in pipeline with callback hook using transactions. 
     * 
     * @param actionId 
     * @param dataContext
     * @param callback
     * @return
     */
    Object txPipeline(final int actionId, final DataContext dataContext,
                      final Consumer<ProcessContext> callback);

}
