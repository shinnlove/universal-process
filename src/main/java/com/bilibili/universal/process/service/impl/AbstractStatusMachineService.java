/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import static com.bilibili.universal.process.consts.MachineConstant.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.core.ProcessBlockingCoreService;
import com.bilibili.universal.process.core.ProcessStatusCoreService;
import com.bilibili.universal.process.core.UniversalProcessCoreService;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.blocking.ProcessBlocking;
import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.model.status.StatusRefMapping;
import com.bilibili.universal.process.no.SnowflakeIdWorker;
import com.bilibili.universal.process.service.ActionExecutor;
import com.bilibili.universal.process.service.ProcessMetadataService;
import com.bilibili.universal.process.service.StatusMachine2ndService;
import com.bilibili.universal.process.wrap.ReflectWrapWithResult;
import com.bilibili.universal.util.code.SystemCode;
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

    private static final Logger      logger       = LoggerFactory
        .getLogger(AbstractStatusMachineService.class);

    /** search fields */
    private static final Set<String> searchFields = new HashSet<>();

    static {
        searchFields.add(OPERATOR_TYPE);
        searchFields.add(OPERATOR_ID);
        searchFields.add(OPERATOR);
        searchFields.add(REMARK);
    }

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
    protected ProcessMetadataService    processMetadataService;

    /** universal process core service */
    @Autowired
    private UniversalProcessCoreService universalProcessCoreService;

    /** process status core service */
    @Autowired
    private ProcessStatusCoreService    processStatusCoreService;

    /** process blocking core service */
    @Autowired
    private ProcessBlockingCoreService  processBlockingCoreService;

    protected UniversalProcess queryNoProcess(long processNo) {
        return universalProcessCoreService.getProcessByNo(processNo);
    }

    protected UniversalProcess queryRefProcess(long refUniqueNo) {
        return universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo, false);
    }

    protected UniversalProcess existRefProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        AssertUtil.isNotNull(process);

        return process;
    }

    protected void notExistRefProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        AssertUtil.isNull(process);
    }

    protected UniversalProcess lockRefProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            true);
        AssertUtil.isNotNull(process, SystemCode.PARAM_INVALID,
            MachineConstant.NO_PROCESS_IN_SYSTEM);
        return process;
    }

    protected long createProcess(int templateId, long processNo, long refUniqueNo,
                                 long parentRefUniqueNo, int source, int destination,
                                 int operatorType, long operatorId, String operator,
                                 String remark) {
        return processStatusCoreService.createProcessWithStatus(templateId, processNo, refUniqueNo,
            parentRefUniqueNo, source, destination, operatorType, operatorId, operator, remark);
    }

    protected long proceedProcessStatus(int templateId, int actionId, long processNo, int source,
                                        int destination, int operatorType, long operatorId,
                                        String operator, String remark) {
        return processStatusCoreService.proceedProcessStatus(templateId, actionId, processNo,
            source, destination, operatorType, operatorId, operator, remark);
    }

    protected List<ProcessBlocking> blockingByNo(long processNo) {
        return processBlockingCoreService.getBlockingByProcessNo(processNo);
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
                    throw new SystemException(SystemCode.SYSTEM_ERROR,
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
            throw new SystemException(SystemCode.SYSTEM_ERROR, e, e.getMessage());
        }
    }

    protected <R> R tx(final Function<TransactionStatus, R> function) {
        return transactionTemplate.execute(status -> function.apply(status));
    }

    protected void runAsync(final ProcessContext context, final List<ActionHandler> handlers) {
        if (!CollectionUtils.isEmpty(handlers)) {
            CompletableFuture.runAsync(() -> execute(context, handlers), asyncExecutor);
        }
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
        // current context ref unique no
        context.setRefUniqueNo(refUniqueNo);
        context.setSourceStatus(source);
        context.setDestinationStatus(destination);

        // prevent NPE
        if (Objects.isNull(dataContext)) {
            dataContext = new DataContext();
        } else {
            // directly copy
            reflectCopyData(dataContext.getData(), dataContext);
        }
        context.setDataContext(dataContext);

        // VIP: back fill
        dataContext.setRefUniqueNo(refUniqueNo);

        return context;
    }

    @SuppressWarnings("rawtypes")
    protected ProcessContext buildContext(int templateId, long refUniqueNo, long parentRefUniqueNo,
                                          int source, int destination, DataContext dataContext) {
        return buildContext(templateId, -1, refUniqueNo, parentRefUniqueNo, source, destination,
            dataContext);
    }

    @SuppressWarnings("rawtypes")
    protected ProcessContext buildContext(int templateId, int actionId, long refUniqueNo,
                                          long parentRefUniqueNo, int source, int destination,
                                          DataContext dataContext) {
        ProcessContext context = buildContext(templateId, actionId, refUniqueNo, source,
            destination, dataContext);

        // just for initialize:
        // Case A: when has parent process, set parent ref unique no for children to read 
        if (parentRefUniqueNo > 0) {
            ProcessContext c = new ProcessContext();
            c.setRefUniqueNo(parentRefUniqueNo);
            context.setParentContext(c);
        }

        // Case B: when has children processes, set children unique no for parent to read
        if (!CollectionUtils.isEmpty(dataContext.getChildrenDataContext())) {
            Map<Integer, DataContext> children = dataContext.getChildrenDataContext();
            for (Map.Entry<Integer, DataContext> entry : children.entrySet()) {
                int tplId = entry.getKey();
                DataContext child = entry.getValue();

                ProcessContext c = new ProcessContext();
                c.setRefUniqueNo(child.getRefUniqueNo());

                context.addChildContext(tplId, c);
            }
        }

        return context;
    }

    protected void backFillOpInfo(DataContext dataContext, int operatorType, long operatorId,
                                  String operator, String remark) {
        dataContext.setOperatorType(operatorType);
        dataContext.setOperatorId(operatorId);
        dataContext.setOperator(operator);
        dataContext.setRemark(remark);
    }

    @SuppressWarnings("rawtypes")
    private void reflectCopyData(Object bizData, DataContext dataContext) {
        if (Objects.nonNull(bizData) && Objects.nonNull(bizData.getClass())
            && !bizData.getClass().equals(Object.class)) {

            Class<?> clazz = bizData.getClass();
            Field[] fields = clazz.getDeclaredFields();

            for (Field f : fields) {
                String fName = f.getName();
                if (!searchFields.contains(fName)) {
                    continue;
                }
                Object v = fValue(bizData, f);

                try {
                    if (OPERATOR_TYPE.equals(fName)) {
                        dataContext.setOperatorType(Integer.parseInt(String.valueOf(v)));
                    } else if (OPERATOR_ID.equals(fName)) {
                        dataContext.setOperatorId(Long.parseLong(String.valueOf(v)));
                    } else if (OPERATOR.equals(fName)) {
                        dataContext.setOperator(String.valueOf(v));
                    } else if (REMARK.equals(fName)) {
                        dataContext.setRemark(String.valueOf(v));
                    }
                } catch (Exception e) {

                }
            }
        }
    }

    private Object fValue(final Object o, final Field f) {
        if (o == null || f == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(f);
        return reflect(() -> f.get(o));
    }

    private Object reflect(ReflectWrapWithResult wrap) {
        try {
            return wrap.call();
        } catch (IllegalAccessException e) {

        } catch (NoSuchFieldException e) {

        } catch (Exception e) {

        }
        return null;
    }

    protected void checkSourceStatus(int currentStatus, int source) {
        // -1 represents universal status.
        if (source != -1 && currentStatus != source) {
            throw new SystemException(SystemCode.PARAM_INVALID,
                MachineConstant.SOURCE_STATUS_INCORRECT);
        }
    }

    protected int realSrc(int currentStatus, int source) {
        return source == DEFAULT_STATUS ? currentStatus : source;
    }

}