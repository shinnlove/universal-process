/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service.impl;

import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.bilibili.universal.process.consts.MachineConstant;
import com.bilibili.universal.process.core.ProcessBlockingCoreService;
import com.bilibili.universal.process.core.ProcessStatusCoreService;
import com.bilibili.universal.process.core.UniversalProcessCoreService;
import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.process.UniversalProcess;
import com.bilibili.universal.process.service.ProcessMetadataService;
import com.bilibili.universal.util.code.SystemResultCode;
import com.bilibili.universal.util.common.AssertUtil;

import javax.annotation.Resource;

/**
 * Resources holding by abstract status machine service.
 * 
 * @author Tony Zhao
 * @version $Id: AbstractStatusMachineService.java, v 0.1 2022-02-23 12:48 PM Tony Zhao Exp $$
 */
public abstract class AbstractStatusMachineService extends AbstractStatusMachineUtilService {

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

    protected UniversalProcess existProcess(long refUniqueNo) {
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        AssertUtil.isNull(process);
        return process;
    }

    protected UniversalProcess lockProcess(long refUniqueNo) {
        UniversalProcess uProcess = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            true);
        AssertUtil.isNotNull(uProcess, SystemResultCode.PARAM_INVALID,
            MachineConstant.NO_PROCESS_IN_SYSTEM);
        return uProcess;
    }

    protected long createProcess(int templateId, int actionId, long processNo, long refUniqueNo,
                                 long parentRefUniqueNo, int source, int destination,
                                 String operator, String remark) {
        return processStatusCoreService.createProcessWithStatus(templateId, actionId, processNo,
            refUniqueNo, parentRefUniqueNo, source, destination, operator, remark);
    }

    protected <R> R tx(final Function<TransactionStatus, R> function) {
        return transactionTemplate.execute(status -> function.apply(status));
    }

}