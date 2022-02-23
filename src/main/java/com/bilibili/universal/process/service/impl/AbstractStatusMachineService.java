/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.core.ProcessBlockingCoreService;
import com.bilibili.universal.process.core.ProcessStatusCoreService;
import com.bilibili.universal.process.core.UniversalProcessCoreService;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.blocking.ProcessBlocking;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.no.SnowflakeIdWorker;
import com.bilibili.universal.process.service.ActionExecutor;
import com.bilibili.universal.process.service.ProcessMetadataService;
import com.bilibili.universal.util.code.SystemResultCode;
import com.bilibili.universal.util.common.AssertUtil;
import com.bilibili.universal.util.exception.SystemException;
import com.bilibili.universal.util.log.LoggerUtil;

import javax.annotation.Resource;

/**
 * Resources holding by abstract status machine service.
 * 
 * @author Tony Zhao
 * @version $Id: AbstractStatusMachineService.java, v 0.1 2022-02-23 12:48 PM Tony Zhao Exp $$
 */
public abstract class AbstractStatusMachineService extends AbstractStatusMachineUtilService {

    private static final Logger         logger = LoggerFactory
        .getLogger(AbstractStatusMachineService.class);

    /** thread executor */
    @Autowired
    @Qualifier("processPool")
    private ExecutorService             asyncExecutor;

    /** snowflake id generator */
    @Autowired
    @Qualifier("snowFlakeId")
    private SnowflakeIdWorker           snowflakeIdWorker;

    /** an executor to execute a couple of action's handlers */
    @Autowired
    private ActionExecutor              actionExecutor;

    /** transaction template */
    @Resource
    private TransactionTemplate         transactionTemplate;

    /** universal cache metadata service */
    @Autowired
    private ProcessMetadataService      processMetadataService;

    /** universal process core service */
    @Autowired
    private UniversalProcessCoreService universalProcessCoreService;

    /** process status core service */
    @Autowired
    private ProcessStatusCoreService    processStatusCoreService;

    /** process blocking core service */
    @Autowired
    private ProcessBlockingCoreService  processBlockingCoreService;

    protected int getDst(int templateId) {
        return processMetadataService.getDstByTemplateId(templateId);
    }

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

    protected List<ActionHandler> handlers(int actionId, boolean isSync) {
        List<ActionHandler> syncHandlers = processMetadataService.getExecutions(actionId, isSync);
        return syncHandlers;
    }

    protected UniversalProcess queryNoProcess(long processNo) {
        return universalProcessCoreService.getProcessByNo(processNo);
    }

    protected UniversalProcess existRefProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        AssertUtil.isNull(process);
        return process;
    }

    protected UniversalProcess lockRefProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            true);
        AssertUtil.isNotNull(process, SystemResultCode.PARAM_INVALID,
            MachineConstant.NO_PROCESS_IN_SYSTEM);
        return process;
    }

    protected UniversalProcess lockNoProcess(long processNo) {
        return universalProcessCoreService.lockProcessByProcessNo(processNo);
    }

    protected long createProcess(int templateId, int actionId, long processNo, long refUniqueNo,
                                 long parentRefUniqueNo, int source, int destination,
                                 String operator, String remark) {
        return processStatusCoreService.createProcessWithStatus(templateId, actionId, processNo,
            refUniqueNo, parentRefUniqueNo, source, destination, operator, remark);
    }

    protected long proceedProcessStatus(int templateId, int actionId, long processNo, int source,
                                        int destination, String operator, String remark) {
        return processStatusCoreService.proceedProcessStatus(templateId, actionId, processNo,
            source, destination, operator, remark);
    }

    protected List<ProcessBlocking> blockingByNo(long processNo) {
        return processBlockingCoreService.getBlockingByProcessNo(processNo);
    }

    protected List<UniversalProcess> siblingsByNo(long parentNo, long selfNo) {
        List<UniversalProcess> childProcesses = universalProcessCoreService
            .getProcessListByParentProcessNo(parentNo);
        List<UniversalProcess> otherChildProcessList = childProcesses.stream()
            .filter(t -> t.getProcessNo() != selfNo).collect(Collectors.toList());

        return otherChildProcessList;
    }

    protected int getACStatus(int templateId) {
        return processMetadataService.getACStatus(templateId);
    }

    protected boolean isFinalStatus(int templateId, int status) {
        return processMetadataService.isFinalStatus(templateId, status);
    }

    protected void execute(final ProcessContext context,
                           final List<ActionHandler> handlers) throws SystemException {
        // real execute action's handlers
        try {
            actionExecutor.proceed(context, handlers);
        } catch (Exception e) {
            LoggerUtil.error(logger, e, "Handler execution has error occurred, ", e.getMessage());
            // special warn: according to meeting discussion, the whole tx should be interrupted if one handler execute failed.
            throw new SystemException(SystemResultCode.SYSTEM_ERROR, e, e.getMessage());
        }
    }

    protected <R> R tx(final Function<TransactionStatus, R> function) {
        return transactionTemplate.execute(status -> function.apply(status));
    }

    protected void runAsync(Runnable r) {
        CompletableFuture.runAsync(r, asyncExecutor);
    }

    protected long nextId() {
        return snowflakeIdWorker.nextId();
    }

}