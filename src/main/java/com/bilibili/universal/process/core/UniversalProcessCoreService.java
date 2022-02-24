/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.core;

import java.util.List;

import com.bilibili.universal.process.model.process.UniversalProcess;

/**
 * The core service interface of process machine.
 *
 * @description 流程状态服务
 * @author caowei
 * @date 2021/7/9 7:07 下午
 **/
public interface UniversalProcessCoreService {

    /**
     * insert and start an universal process.
     *
     * @param universalProcess
     * @return
     */
    long addProcess(UniversalProcess universalProcess);

    /**
     * Get process by unique process no.
     *
     * @param processNo     the unique process number generated by snowflake algorithm.
     * @return
     */
    UniversalProcess getProcessByNo(long processNo);

    /**
     * Get process list by given reference ids, i.e. list of mission ids.
     *
     * @param refUniqueNos      mission unique No.s
     * @return
     */
    List<UniversalProcess> getProcessListByRefUniqueNos(List<Long> refUniqueNos);

    /**
     * Get process list by given parent process No.
     *
     * @param parentRefUniqueNo         the parent ref unique No.
     * @return
     */
    List<UniversalProcess> getProcessListByParentRefUniqueNo(long parentRefUniqueNo);

    /**
     * Get and lock one process by ref id.
     * upper => B/C => enroll id => refId
     *
     * mission => operator publish => mission_id => refId
     *
     * @param refUniqueNo         refer to other business's unique No. the unique business id, i.e.: enroll id, mission id or other mapping key id.
     * @param lock                if need lock in select for update mode
     * @return
     */
    UniversalProcess getProcessByRefUniqueNo(long refUniqueNo, boolean lock);

    /**
     * Get and lock process by the unique process No.
     *
     * @param processNo     the unique process number of a process.
     * @return
     */
    UniversalProcess lockProcessByProcessNo(long processNo);

    /**
     * Update process status to target, called by status log core service.
     *
     * @param processNo             processNo.
     * @param sourceStatus          source status
     * @param destinationStatus     destination status
     * @return
     */
    int updateProcessStatus(long processNo, int sourceStatus, int destinationStatus);

}
