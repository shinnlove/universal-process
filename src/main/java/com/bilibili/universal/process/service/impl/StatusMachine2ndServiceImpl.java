/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.callback.ProcessCallback;
import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.core.ProcessBlockingCoreService;
import com.bilibili.universal.process.core.ProcessStatusCoreService;
import com.bilibili.universal.process.core.UniversalProcessCoreService;
import com.bilibili.universal.process.enums.TemplateType;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.blocking.ProcessBlocking;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.cache.TemplateMetadata;
import com.bilibili.universal.process.model.cascade.PrepareParent;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.model.process.UniversalProcess;
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
 * @author Tony Zhao
 * @version $Id: StatusMachine2ndServiceImpl.java, v 0.1 2022-02-09 5:50 PM Tony Zhao Exp $$
 */
@Service
public class StatusMachine2ndServiceImpl implements StatusMachine2ndService {

    private static final Logger         logger = LoggerFactory
        .getLogger(StatusMachine2ndServiceImpl.class);

    /** thread executor */
    @Autowired
    @Qualifier("processPool")
    private ExecutorService             asyncExecutor;

    /** snowflake id generator */
    @Autowired
    @Qualifier("snowFlakeId")
    private SnowflakeIdWorker           snowflakeIdWorker;

    /** transaction template */
    @Resource
    private TransactionTemplate         transactionTemplate;

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

    /** an executor to execute a couple of action's handlers */
    @Autowired
    private ActionExecutor              actionExecutor;

    @Override
    public long initProcess(int templateId, long refUniqueNo, DataContext dataContext) {
        return initProcess(templateId, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long initProcess(int templateId, long refUniqueNo, DataContext dataContext,
                            ProcessCallback callback) {
        int dst = TemplateType.getDstByTemplateId(templateId);
        return initProcess(templateId, dst, refUniqueNo, dataContext, callback);
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            DataContext dataContext) {
        return initProcess(templateId, destination, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            DataContext dataContext, ProcessCallback callback) {
        AssertUtil.largeThanValue(refUniqueNo, 0);
        final int source = -1;

        TemplateCache template = processMetadataService.getTemplateById(templateId);
        AssertUtil.isNotNull(template);

        Map<Integer, List<ActionHandler>> inits = template.getInitializers();
        if (CollectionUtils.isEmpty(inits) || !inits.containsKey(destination)) {
            return 0;
        }

        List<ActionHandler> handlers = inits.get(destination);
        if (CollectionUtils.isEmpty(handlers)) {
            return 0;
        }

        // 4th: prepare proceed context
        ProcessContext context = buildProContext(templateId, refUniqueNo, source, destination,
            dataContext);

        // fast query once to check if it's a new process
        UniversalProcess nProcess = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        AssertUtil.isNull(nProcess);

        final long processNo = snowflakeIdWorker.nextId();

        Long processId = transactionTemplate.execute(status -> {
            execute(context, handlers);

            // secondly create new process
            long newProcessId = processStatusCoreService.createProcessWithStatus(templateId, -1,
                processNo, refUniqueNo, source, destination, dataContext.getOperator(),
                dataContext.getRemark());

            // Warning: give callback a chance to execute outta business codes, callback must after execute!
            if (callback != null) {
                callback.doCallback(context);
            }

            return newProcessId;
        });

        AssertUtil.largeThanValue(processId, 0);

        return processNo;
    }

    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext) {
        return proceedProcess(actionId, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                         ProcessCallback callback) {

        TemplateCache template = processMetadataService.getTemplateByActionId(actionId);
        AssertUtil.isNotNull(template);

        TemplateMetadata metadata = template.getMetadata();

        int templateId = metadata.getId();
        int needReconcile = metadata.getCompleteReconcileParent();
        int reconcileMode = metadata.getCoordinateMode();

        Map<Integer, ActionCache> actionMap = template.getActions();
        ActionCache cache = actionMap.get(actionId);
        final int source = cache.getSource();
        final int destination = cache.getDestination();

        // prepare proceed context and handlers
        ProcessContext context = buildProContext(templateId, actionId, refUniqueNo, source,
            destination, dataContext);
        List<ActionHandler> syncHandlers = processMetadataService.getExecutions(actionId, true);
        List<ActionHandler> asyncHandlers = processMetadataService.getExecutions(actionId, false);

        // fast query once to check if it's a new process
        UniversalProcess nProcess = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        AssertUtil.notNull(nProcess);

        // 6th: check current process info if process exists
        Integer result = tx(status -> {

            // need to lock current process
            UniversalProcess uProcess = universalProcessCoreService
                .getProcessByRefUniqueNo(refUniqueNo, true);
            AssertUtil.isNotNull(uProcess, SystemResultCode.PARAM_INVALID,
                MachineConstant.NO_PROCESS_IN_SYSTEM);

            int currentStatus = uProcess.getCurrentStatus();
            long pProcessNo = uProcess.getParentProcessNo();
            long processNo = uProcess.getProcessNo();
            context.setProcessNo(processNo);

            // use status flow reflection to validate state flow correct
            checkSourceStatus(currentStatus, source);

            // check no blocking in way..
            checkProcessBlocking(processNo);

            // real execute action's handlers
            execute(context, syncHandlers);

            // service which operates the combination of status
            processStatusCoreService.proceedProcessStatus(templateId, actionId, processNo, source,
                destination, dataContext.getOperator(), dataContext.getRemark());

            //            reconcileParent(processNo, parentProcessNo, needReconcile, reconcileMode);

            if (pProcessNo > 0) {
                UniversalProcess pProcess = universalProcessCoreService.getProcessByNo(pProcessNo);
                long pRefUniqueNo = pProcess.getRefUniqueNo();
                int ptId = pProcess.getTemplateId();
                int pActionId = chooseAction(ptId, source, destination);
                if (pActionId > 0) {
                    Object pParam = chooseParam(cache, context, ptId);
                    DataContext d = new DataContext(pParam);

                    ProcessContext pContext = proceedProcess(pActionId, pRefUniqueNo, d);
                    LoggerUtil.info(logger, "successfully proceed parent, pContext=", pContext);
                }
            }

            // give a change for business codes to execute outta logic, callback must after execute!
            if (callback != null) {
                callback.doCallback(context);
            }

            return 1;

        }); // 6th execute tx

        AssertUtil.largeThanValue(result, 0);

        // At last, execute async handlers outta transaction!
        CompletableFuture.runAsync(() -> execute(context, asyncHandlers), asyncExecutor);

        return context;
    }

    private int chooseAction(int parentTemplateId, int source, int destination) {
        TemplateCache cache = processMetadataService.getTemplateById(parentTemplateId);
        Map<Integer, Map<Integer, ActionCache>> parentActionDst = cache.getActionTable();
        if (parentActionDst.containsKey(destination)) {
            Map<Integer, ActionCache> dstMapping = parentActionDst.get(destination);
            if (dstMapping.containsKey(source)) {
                ActionCache dstAction = dstMapping.get(source);
                // parent revising action id
                return dstAction.getActionId();
            }
        }
        return -1;
    }

    private Object chooseParam(ActionCache cache, ProcessContext context, int parentTemplateId) {
        PrepareParent prepare = getPrepare(cache.getPrepareHandler());
        if (prepare != null && prepare.getParentTemplateId() == parentTemplateId) {
            // has prepare parameter
            return getResultByClass(context, prepare.getClassName());
        } else {
            // use default if not exist
            List<ActionHandler> syncHandlers = cache.getSyncHandlers();
            int size = syncHandlers.size();
            ActionHandler h = syncHandlers.get(size - 1);
            String name = h.getClass().getName();
            return getResultByClass(context, name);
        }
    }

    private Object getResultByClass(ProcessContext context, String className) {
        Object result = null;
        Map<String, Object> results = context.getResultObject();
        if (!CollectionUtils.isEmpty(results) && results.containsKey(className)) {
            result = results.get(className);
        }
        return result;
    }

    private PrepareParent getPrepare(Map<String, Integer> prepare) {
        for (Map.Entry<String, Integer> entry : prepare.entrySet()) {
            String prepareName = entry.getKey();
            int prepareId = entry.getValue();
            return new PrepareParent(prepareName, prepareId);
        }
        return null;
    }

    private ProcessContext buildProContext(int templateId, long refUniqueNo, int source,
                                           int destination, DataContext dataContext) {
        ProcessContext context = new ProcessContext();
        context.setTemplateId(templateId);
        context.setRefUniqueNo(refUniqueNo);
        context.setSourceStatus(source);
        context.setDestinationStatus(destination);
        context.setDataContext(dataContext);
        return context;
    }

    private ProcessContext buildProContext(int templateId, int actionId, long refUniqueNo,
                                           int source, int destination, DataContext dataContext) {
        ProcessContext context = new ProcessContext();
        context.setTemplateId(templateId);
        context.setActionId(actionId);
        context.setRefUniqueNo(refUniqueNo);
        context.setSourceStatus(source);
        context.setDestinationStatus(destination);
        context.setDataContext(dataContext);
        return context;
    }

    private <R> R tx(final Function<TransactionStatus, R> function) {
        return transactionTemplate.execute(status -> function.apply(status));
    }

    private void execute(final ProcessContext context,
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

    private void checkSourceStatus(int currentStatus, int source) {
        if (currentStatus != source) {
            throw new SystemException(SystemResultCode.PARAM_INVALID,
                MachineConstant.SOURCE_STATUS_INCORRECT);
        }
    }

    /**
     * Check there is no blocking issue or all process have reached final destination status.
     *
     * @param processNo     the specific check processNo.
     */
    private void checkProcessBlocking(long processNo) {
        // check there is no blocking issue or all process have reached final destination status
        List<ProcessBlocking> blockingList = processBlockingCoreService
            .getBlockingByProcessNo(processNo);
        if (!CollectionUtils.isEmpty(blockingList)) {
            for (ProcessBlocking b : blockingList) {
                long bProcessNo = b.getMainProcessNo();
                UniversalProcess bProcess = universalProcessCoreService.getProcessByNo(bProcessNo);
                int btId = bProcess.getTemplateId();
                int bStatus = bProcess.getCurrentStatus();
                if (!processMetadataService.isFinalStatus(btId, bStatus)) {
                    throw new SystemException(SystemResultCode.SYSTEM_ERROR,
                        MachineConstant.STATUS_HAS_BLOCKING_PROCESS);
                }
            }
        } // if blocking
    }

    private void reconcileParent(long selfProcessNo, long parentProcessNo, int needReconcile,
                                 int reconcileMode) {
        // if status reached to end, then check reconcile mode
        if (parentProcessNo != -1 && needReconcile == 1) {
            // get parent process
            UniversalProcess parentProcess = universalProcessCoreService
                .lockProcessByProcessNo(parentProcessNo);
            int pTemplateId = parentProcess.getTemplateId();
            int pActionId = -1;
            int pcStatus = parentProcess.getCurrentStatus();

            // get other sibling process
            List<UniversalProcess> childProcesses = universalProcessCoreService
                .getProcessListByParentProcessNo(parentProcessNo);
            List<UniversalProcess> otherChildProcessList = childProcesses.stream()
                .filter(t -> t.getProcessNo() != selfProcessNo).collect(Collectors.toList());

            // check reconcile flag
            boolean needUpdateParent = true;
            if (reconcileMode == 1) {
                // cooperate mode:
                // loop to check each child process status with no lock and update parent status if ok
                for (UniversalProcess up : otherChildProcessList) {
                    int uptId = up.getTemplateId();
                    int upStatus = up.getCurrentStatus();
                    boolean isFinal = processMetadataService.isFinalStatus(uptId, upStatus);
                    if (!isFinal) {
                        needUpdateParent = false;
                    }
                }
            }

            // need reconcile parent
            if (needUpdateParent) {
                int pacStatus = processMetadataService.getACStatus(pTemplateId);
                processStatusCoreService.proceedProcessStatus(pTemplateId, pActionId,
                    parentProcessNo, pcStatus, pacStatus, MachineConstant.DEFAULT_OPERATOR,
                    MachineConstant.DEFAULT_REMARK);
            }

        } // if need reconcile
    }

}