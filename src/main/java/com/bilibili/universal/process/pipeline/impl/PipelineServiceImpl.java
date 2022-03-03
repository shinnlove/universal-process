/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.pipeline.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.pipeline.PipelineService;
import com.bilibili.universal.process.service.ProcessMetadataService;
import com.bilibili.universal.util.exception.SystemException;
import com.bilibili.universal.util.log.LoggerUtil;

import javax.annotation.Resource;

/**
 * Pipeline service without status control.
 * 
 * @author Tony Zhao
 * @version $Id: PipelineServiceImpl.java, v 0.1 2021-12-28 11:38 PM Tony Zhao Exp $$
 */
@Service
public class PipelineServiceImpl implements PipelineService {

    private static final Logger    logger = LoggerFactory.getLogger(PipelineServiceImpl.class);

    /** pipeline executor. */
    @Resource
    @Qualifier("pipelinePool")
    private ExecutorService        executor;

    /** transaction template */
    @Resource
    private TransactionTemplate    transactionTemplate;

    /** process template and status metadata autowired service */
    @Autowired
    private ProcessMetadataService processMetadataService;

    @Override
    public Object doPipeline(int actionId, DataContext dataContext) {
        return doPipeline(actionId, dataContext, resp -> {
        });
    }

    @Override
    public Object doPipeline(int actionId, DataContext dataContext,
                             Consumer<ProcessContext> callback) {
        ProcessContext<String> context = new ProcessContext<>(dataContext);

        // prepare handlers
        List<ActionHandler> syncHandlers = processMetadataService.getExecutions(actionId, true);

        execute(context, syncHandlers);

        if (callback != null) {
            callback.accept(context);
        }

        return context;
    }

    @Override
    public Object txPipeline(int actionId, DataContext dataContext) {
        return txPipeline(actionId, dataContext, resp -> {
        });
    }

    @Override
    public Object txPipeline(int actionId, DataContext dataContext,
                             Consumer<ProcessContext> callback) {
        return tx(status -> doPipeline(actionId, dataContext, callback));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object execute(final ProcessContext context,
                           final List<ActionHandler> handlers) throws SystemException {

        CompletableFuture<Object> f = CompletableFuture.completedFuture(null);
        if (CollectionUtils.isEmpty(handlers)) {
            return doExecute(f);
        }

        int cursor = 0;
        while (cursor < handlers.size()) {
            final int i = cursor++;
            final ActionHandler handler = handlers.get(i);
            f = f.thenCompose(previous -> CompletableFuture.supplyAsync(() -> {
                if (i > 0) {
                    handler.cache(handlers, i - 1, previous, context);
                }
                return handler.pipeline(context);
            }, executor));
        }

        return doExecute(f);
    }

    @SuppressWarnings("rawtypes")
    public Object doExecute(CompletableFuture future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            LoggerUtil.error(logger, e, e);
        } catch (Exception e) {
            LoggerUtil.error(logger, e, e);
        }
        return null;
    }

    protected <R> R tx(final Function<TransactionStatus, R> function) {
        return transactionTemplate.execute(status -> function.apply(status));
    }

}