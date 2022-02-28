/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.core.ProcessBlockingCoreService;
import com.bilibili.universal.process.core.ProcessStatusCoreService;
import com.bilibili.universal.process.core.UniversalProcessCoreService;
import com.bilibili.universal.process.enums.TemplateTriggerType;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.blocking.ProcessBlocking;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.StatusCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.model.status.StatusRefMapping;
import com.bilibili.universal.process.no.SnowflakeIdWorker;
import com.bilibili.universal.process.service.ActionExecutor;
import com.bilibili.universal.process.service.ProcessMetadataService;
import com.bilibili.universal.process.service.StatusMachine2ndService;
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
public abstract class AbstractStatusMachineService implements StatusMachine2ndService {

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
        return processMetadataService.getExecutions(actionId, isSync);
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

    protected UniversalProcess queryNoProcess(long processNo) {
        return universalProcessCoreService.getProcessByNo(processNo);
    }

    protected UniversalProcess queryRefProcess(long refUniqueNo) {
        return universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo, false);
    }

    protected void existRefProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        AssertUtil.isNotNull(process);
    }

    protected void notExistRefProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        AssertUtil.isNull(process);
    }

    protected UniversalProcess lockRefProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            true);
        AssertUtil.isNotNull(process, SystemResultCode.PARAM_INVALID,
            MachineConstant.NO_PROCESS_IN_SYSTEM);
        return process;
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

    protected List<UniversalProcess> refSiblings(long parentRefNo, long selfNo) {
        List<UniversalProcess> childProcesses = refChildren(parentRefNo);
        List<UniversalProcess> otherChildProcessList = childProcesses.stream()
            .filter(t -> t.getProcessNo() != selfNo).collect(Collectors.toList());

        return otherChildProcessList;
    }

    protected List<UniversalProcess> refChildren(long parentRefUniqueNo) {
        return universalProcessCoreService.getProcessListByParentRefUniqueNo(parentRefUniqueNo);
    }

    protected StatusRefMapping statusRefMapping(int parentTemplateId, int childTemplateId) {
        return processMetadataService.getRefStatusMapping(parentTemplateId, childTemplateId);
    }

    protected boolean isFinalStatus(int templateId, int status) {
        return processMetadataService.isFinalStatus(templateId, status);
    }

    protected boolean isParentTpl(int templateId) {
        return processMetadataService.isParentTpl(templateId);
    }

    /**
     * Check there is no blocking issue or all process have reached final destination status.
     *
     * @param processNo     the specific check processNo.
     */
    protected void checkBlocking(long processNo) {
        // check there is no blocking issue or all process have reached final destination status
        List<ProcessBlocking> blockingList = blockingByNo(processNo);
        if (!CollectionUtils.isEmpty(blockingList)) {
            for (ProcessBlocking b : blockingList) {
                long bProcessNo = b.getMainProcessNo();
                UniversalProcess bProcess = queryNoProcess(bProcessNo);
                int btId = bProcess.getTemplateId();
                int bStatus = bProcess.getCurrentStatus();
                if (!isFinalStatus(btId, bStatus)) {
                    throw new SystemException(SystemResultCode.SYSTEM_ERROR,
                        MachineConstant.STATUS_HAS_BLOCKING_PROCESS);
                }
            }
        } // if blocking
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected ProcessContext buildContext(int templateId, long refUniqueNo, int source,
                                          int destination, DataContext dataContext) {
        return buildContext(templateId, -1, refUniqueNo, source, destination, dataContext);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected ProcessContext buildContext(int templateId, int actionId, long refUniqueNo,
                                          int source, int destination, DataContext dataContext) {
        ProcessContext context = new ProcessContext();
        context.setTemplateId(templateId);
        context.setActionId(actionId);
        context.setRefUniqueNo(refUniqueNo);
        context.setSourceStatus(source);
        context.setDestinationStatus(destination);

        // prevent NPE
        if (Objects.isNull(dataContext)) {
            dataContext = new DataContext();
        }
        context.setDataContext(dataContext);
        return context;
    }

    protected void checkSourceStatus(int currentStatus, int source) {
        // -1 represents universal status.
        if (source != -1 && currentStatus != source) {
            throw new SystemException(SystemResultCode.PARAM_INVALID,
                MachineConstant.SOURCE_STATUS_INCORRECT);
        }
    }

}