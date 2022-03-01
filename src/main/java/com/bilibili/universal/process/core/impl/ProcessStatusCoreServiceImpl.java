/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.core.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bilibili.universal.process.core.ProcessStatusCoreService;
import com.bilibili.universal.process.core.ProcessStatusLogCoreService;
import com.bilibili.universal.process.core.UniversalProcessCoreService;
import com.bilibili.universal.process.model.log.ProcessStatusLog;
import com.bilibili.universal.process.model.process.UniversalProcess;

/**
 * Implementation of core service mixed of universal process status and process log core service.
 *
 * @author Tony Zhao
 * @version $Id: ProcessStatusCoreServiceImpl.java, v 0.1 2021-07-20 8:00 PM Tony Zhao Exp $$
 */
@Service
public class ProcessStatusCoreServiceImpl implements ProcessStatusCoreService {

    @Autowired
    private UniversalProcessCoreService universalProcessCoreService;

    @Autowired
    private ProcessStatusLogCoreService processStatusLogCoreService;

    @Override
    public long createProcessWithStatus(int templateId, long processNo, long refUniqueNo,
                                        long parentRefUniqueNo, int source, int destination,
                                        int operatorType, long operatorId, String operator,
                                        String remark) {
        // build model
        UniversalProcess uProcess = new UniversalProcess();
        uProcess.setTemplateId(templateId);
        uProcess.setProcessNo(processNo);
        uProcess.setRefUniqueNo(refUniqueNo);
        uProcess.setParentRefUniqueNo(parentRefUniqueNo);

        uProcess.setCurrentStatus(destination);

        uProcess.setLatestOperatorType(operatorType);
        uProcess.setLatestOperatorId(operatorId);
        uProcess.setLatestOperator(operator);

        uProcess.setRemark(remark);

        long processId = universalProcessCoreService.addProcess(uProcess);

        ProcessStatusLog statusLog = new ProcessStatusLog(processNo, templateId, -1, source,
            destination, operatorType, operatorId, operator, remark);
        processStatusLogCoreService.insertProcessLog(statusLog);

        return processId;
    }

    @Override
    public long proceedProcessStatus(int templateId, int actionId, long processNo, int source,
                                     int destination, int operatorType, long operatorId,
                                     String operator, String remark) {
        // update process status
        universalProcessCoreService.updateProcessStatus(processNo, source, destination);

        // add log after status updated
        ProcessStatusLog statusLog = new ProcessStatusLog(processNo, templateId, actionId, source,
            destination, operatorType, operatorId, operator, remark);
        return processStatusLogCoreService.insertProcessLog(statusLog);
    }

    @Override
    public List<ProcessStatusLog> getLogsByRefUniqueNo(long refUniqueNo) {
        // first use reference unique No. to query process info
        UniversalProcess process = universalProcessCoreService.getProcessByRefUniqueNo(refUniqueNo,
            false);
        if (process == null) {
            return Collections.emptyList();
        }

        // then use process No. to query log ids.
        return processStatusLogCoreService.getLogsByProcessNo(process.getProcessNo());
    }

}