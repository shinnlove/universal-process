/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.bilibili.universal.dao.ProcessStatusLogDao;
import com.bilibili.universal.dao.model.ProcessStatusLogPo;
import com.bilibili.universal.dao.model.ProcessStatusLogPoExample;
import com.bilibili.universal.process.core.ProcessStatusLogCoreService;
import com.bilibili.universal.process.model.log.ProcessStatusLog;
import com.bilibili.universal.util.code.SystemResultCode;
import com.bilibili.universal.util.common.AssertUtil;
import com.bilibili.universal.util.common.CommonUtil;

/**
 * Process status log core service implementation.
 *
 * @author Tony Zhao
 * @version $Id: ProcessStatusLogCoreServiceImpl.java, v 0.1 2021-07-07 3:04 PM Tony Zhao Exp $$
 */
@Service
public class ProcessStatusLogCoreServiceImpl implements ProcessStatusLogCoreService {

    @Autowired
    private ProcessStatusLogDao processStatusLogDao;

    @Override
    public long insertProcessLog(ProcessStatusLog statusLog) {
        ProcessStatusLogPo po = CommonUtil.copyObj(statusLog, ProcessStatusLogPo.class);
        processStatusLogDao.insertSelective(po);
        return po.getId();
    }

    @Override
    public List<ProcessStatusLog> getLogsByProcessNo(long processNo) {

        ProcessStatusLogPoExample example = new ProcessStatusLogPoExample();
        ProcessStatusLogPoExample.Criteria criteria = example.or();
        criteria.andProcessNoEqualTo(processNo);

        example.setOrderByClause("ctime desc");

        List<ProcessStatusLogPo> statusLogPos = processStatusLogDao.selectByExample(example);
        if (CollectionUtils.isEmpty(statusLogPos)) {
            return Collections.emptyList();
        }

        List<ProcessStatusLog> statusLogs = new ArrayList<>();
        for (ProcessStatusLogPo po : statusLogPos) {
            ProcessStatusLog psLog = new ProcessStatusLog();
            BeanUtils.copyProperties(po, psLog);
            statusLogs.add(psLog);
        }

        return statusLogs;
    }

    @Override
    public List<ProcessStatusLog> getLogsByProcessNoAndDesStatus(List<Long> processNos,
                                                                 Integer destinationStatus) {
        AssertUtil.listNotEmpty(processNos, SystemResultCode.PARAM_INVALID);
        AssertUtil.isNotNull(destinationStatus, SystemResultCode.PARAM_INVALID);

        ProcessStatusLogPoExample example = new ProcessStatusLogPoExample();
        example.or().andProcessNoIn(processNos);
        List<ProcessStatusLogPo> pos = processStatusLogDao.selectByExample(example);
        return CommonUtil.copyList(pos, ProcessStatusLog.class);
    }

}