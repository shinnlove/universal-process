/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.pipeline.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.ex.BizHandlerExecuteException;
import com.bilibili.universal.process.ex.BizNPEParamOrResultsException;
import com.bilibili.universal.process.ex.StatusBreakException;
import com.bilibili.universal.process.ex.StatusContinueException;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.pipeline.PipelineService;
import com.bilibili.universal.process.service.ProcessMetadataService;
import com.bilibili.universal.util.code.SystemCode;
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

    /**
     * @see PipelineService#doPipeline(int, DataContext)
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public Object doPipeline(int actionId, DataContext dataContext) {
        return doPipeline(actionId, dataContext, resp -> {
        });
    }

    /**
     * @see PipelineService#doPipeline(int, DataContext, Consumer)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object doPipeline(int actionId, DataContext dataContext,
                             Consumer<ProcessContext> callback) {
        ProcessContext<Object> context = new ProcessContext<>(dataContext);

        // prepare handlers
        List<ActionHandler> syncHandlers = processMetadataService.getExecutions(actionId, true);
        List<ActionHandler> asyncHandlers = processMetadataService.getExecutions(actionId, false);

        // 1st. execute sync handlers
        Object r = execute(context, syncHandlers);

        // 2nd. execute the callback hook
        if (callback != null) {
            callback.accept(context);
        }

        // 3rd. execute async handlers asynchronously without the results
        async(() -> execute(context, asyncHandlers, r), executor);

        return context;
    }

    /**
     * @see PipelineService#txPipeline(int, DataContext)
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public Object txPipeline(int actionId, DataContext dataContext) {
        return txPipeline(actionId, dataContext, resp -> {
        });
    }

    /**
     * @see PipelineService#txPipeline(int, DataContext, Consumer)
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public Object txPipeline(int actionId, DataContext dataContext,
                             Consumer<ProcessContext> callback) {
        return tx(status -> doPipeline(actionId, dataContext, callback));
    }

    @SuppressWarnings({ "rawtypes" })
    private Object execute(final ProcessContext context,
                           final List<ActionHandler> handlers) throws SystemException {
        // when sync handlers execute first, initial result should be null if empty..
        return execute(context, handlers, null);
    }

    /**
     * When initial result is not null, should cache handler's temporarily execute result for next one to use.
     *
     * @param context  execute context
     * @param handlers execute handlers
     * @param initial  execution chain initial results for each handler to use
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object execute(final ProcessContext context, final List<ActionHandler> handlers,
                           final Object initial) {

        CompletableFuture<Object> f = CompletableFuture.completedFuture(initial);
        if (CollectionUtils.isEmpty(handlers)) {
            return doExecute(f);
        }

        int cursor = 0;
        while (cursor < handlers.size()) {
            final int i = cursor++;
            final ActionHandler handler = handlers.get(i);
            f = f.thenCompose(previous -> CompletableFuture.supplyAsync(() -> {
                if (i > 0 || initial != null) {
                    handler.cache(handlers, i - 1, previous, context);
                }
                return handler.pipeline(context);
            }, executor)).exceptionally(this::handleEx);
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

    private Object handleEx(Throwable e) {

        LoggerUtil.error(logger, e, "pipeline execution error occurs, ex=", e.getMessage());

        if (e instanceof NullPointerException) {
            // for smart cache NPE
            throw new BizNPEParamOrResultsException(SystemCode.BIZ_PARAM_RESULT_NPE, e,
                e.getMessage());
        } else if (e instanceof StatusContinueException || e instanceof StatusBreakException) {
            return null;
        } else {
            // for last ex info catch
            throw new BizHandlerExecuteException(SystemCode.BIZ_HANDLER_EXECUTE_ERROR, e,
                e.getMessage());
        }
    }

    private <R> R tx(final Function<TransactionStatus, R> function) {
        return transactionTemplate.execute(function::apply);
    }

    private <T> void async(Supplier<T> callable, Executor executor) {
        CompletableFuture
            .runAsync(() -> doExecute(CompletableFuture.supplyAsync(callable, executor)), executor);
    }

}