/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.batch.BatchInitParam;
import com.bilibili.universal.process.model.batch.BatchInitResult;
import com.bilibili.universal.process.model.batch.InitParam;
import com.bilibili.universal.process.model.blocking.ProcessBlocking;
import com.bilibili.universal.process.model.cache.ActionCache;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.cache.TemplateMetadata;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.no.SnowflakeIdWorker;
import com.bilibili.universal.process.service.ActionExecutor;
import com.bilibili.universal.process.util.TplUtil;
import com.bilibili.universal.util.code.SystemResultCode;
import com.bilibili.universal.util.common.AssertUtil;
import com.bilibili.universal.util.exception.SystemException;
import com.bilibili.universal.util.log.LoggerUtil;

/**
 * @author Tony Zhao
 * @version $Id: StatusMachine2ndServiceImpl.java, v 0.1 2022-02-09 5:50 PM Tony Zhao Exp $$
 */
@Service
public class StatusMachine2ndServiceImpl extends AbstractStatusMachineService {

    private static final Logger logger = LoggerFactory.getLogger(StatusMachine2ndServiceImpl.class);

    /** thread executor */
    @Autowired
    @Qualifier("processPool")
    private ExecutorService     asyncExecutor;

    /** snowflake id generator */
    @Autowired
    @Qualifier("snowFlakeId")
    private SnowflakeIdWorker   snowflakeIdWorker;

    /** an executor to execute a couple of action's handlers */
    @Autowired
    private ActionExecutor      actionExecutor;

    @Override
    public long initProcess(int templateId, long refUniqueNo, DataContext dataContext) {
        return initProcess(templateId, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long initProcess(int templateId, long refUniqueNo, DataContext dataContext,
                            Consumer<ProcessContext> callback) {
        int dst = getDst(templateId);
        return initProcess(templateId, dst, refUniqueNo, dataContext, callback);
    }

    @Override
    public long initProcess(int templateId, long refUniqueNo, long parentRefUniqueNo,
                            DataContext dataContext) {
        return initProcess(templateId, refUniqueNo, parentRefUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long initProcess(int templateId, long refUniqueNo, long parentRefUniqueNo,
                            DataContext dataContext, Consumer<ProcessContext> callback) {
        int dst = getDst(templateId);
        return initProcess(templateId, dst, refUniqueNo, parentRefUniqueNo, dataContext, callback);
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            DataContext dataContext) {
        return initProcess(templateId, destination, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            DataContext dataContext, Consumer<ProcessContext> callback) {
        return initProcess(templateId, destination, refUniqueNo, -1, dataContext, callback);
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            long parentRefUniqueNo, DataContext dataContext) {
        return initProcess(templateId, destination, refUniqueNo, parentRefUniqueNo, dataContext,
            resp -> {
            });
    }

    @Override
    public long initProcess(int templateId, int destination, long refUniqueNo,
                            long parentRefUniqueNo, DataContext dataContext,
                            Consumer<ProcessContext> callback) {
        AssertUtil.largeThanValue(refUniqueNo, 0);
        final int source = -1;

        TemplateCache template = getCache(templateId);

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
        existProcess(refUniqueNo);

        final long processNo = snowflakeIdWorker.nextId();

        Long processId = tx(status -> {
            execute(context, handlers);

            // secondly create new process
            long newProcessId = createProcess(templateId, -1, processNo, refUniqueNo,
                parentRefUniqueNo, source, destination, dataContext.getOperator(),
                dataContext.getRemark());

            // Warning: give callback a chance to execute outta business codes, callback must after execute!
            if (callback != null) {
                callback.accept(context);
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
                                         Consumer<ProcessContext> callback) {
        // default proceed behavior is delegate proceed parent if exists parent process...
        return proceedProcess(actionId, refUniqueNo, dataContext, callback, true);
    }

    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                         boolean proceedParent) {
        return proceedProcess(actionId, refUniqueNo, dataContext, resp -> {
        }, proceedParent);
    }

    @Override
    public ProcessContext proceedProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                         Consumer<ProcessContext> callback, boolean proceedParent) {
        TemplateCache template = getTpl(actionId);
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
        List<ActionHandler> syncHandlers = handlers(actionId, true);
        List<ActionHandler> asyncHandlers = handlers(actionId, false);

        // fast query once to check if it's a new process
        existProcess(refUniqueNo);

        // 6th: check current process info if process exists
        Integer result = tx(status -> {

            // need to lock current process
            UniversalProcess uProcess = lockProcess(refUniqueNo);

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

            if (pProcessNo > 0 && proceedParent) {
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
                callback.accept(context);
            }

            return 1;

        }); // 6th execute tx

        AssertUtil.largeThanValue(result, 0);

        // At last, execute async handlers outta transaction!
        CompletableFuture.runAsync(() -> execute(context, asyncHandlers), asyncExecutor);

        return context;
    }

    @Override
    public BatchInitResult batchInitProcess(BatchInitParam param) {
        return batchInitProcess(param, resp -> {
        });
    }

    @Override
    public BatchInitResult batchInitProcess(BatchInitParam param,
                                            Consumer<ProcessContext> callback) {
        // add some validator here..

        List<InitParam> inits = param.getParams();
        InitParam init = inits.get(0);
        int ctId = init.getTemplateId();

        TemplateCache cache = processMetadataService.getTemplateById(ctId);
        int ptId = TplUtil.parentId(cache);

        DataContext pData = param.getParentDataContext();

        long pRefNo = param.getParentRefUniqueNo();
        if (pRefNo == -1) {
            pRefNo = snowflakeIdWorker.nextId();
        }

        final long pno = pRefNo;
        final Map<Integer, Long> processNos = new HashMap<>();
        tx(status -> {

            inits.forEach(i -> {
                int id = i.getTemplateId();
                long cRefNo = i.getRefUniqueNo();
                DataContext cd = i.getDataContext();

                long no = initProcess(id, cRefNo, pno, cd);
                processNos.put(id, no);
            });

            return initProcess(ptId, pno, pData);
        });

        return new BatchInitResult(pno, processNos);
    }

    @Override
    public long proceedParentProcess(int actionId, long refUniqueNo, DataContext dataContext) {
        return proceedParentProcess(actionId, refUniqueNo, dataContext, resp -> {
        });
    }

    @Override
    public long proceedParentProcess(int actionId, long refUniqueNo, DataContext dataContext,
                                     Consumer<ProcessContext> callback) {
        return 1L;
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