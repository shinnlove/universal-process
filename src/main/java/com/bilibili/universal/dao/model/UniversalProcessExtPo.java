/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.dao.model;

import java.io.Serializable;
import java.sql.Timestamp;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author Tony Zhao
 * @version $Id: UniversalProcessExtPo.java, v 0.1 2022-03-04 5:28 PM Tony Zhao Exp $$
 */
public class UniversalProcessExtPo implements Serializable {

    private static final long serialVersionUID = 2328112101565709032L;

    /**
     * 主键id.
     */
    private Long              id;

    /**
     * 流程编号
     */
    private Long              processNo;

    /**
     * 流程所属业务: 1-自助改价流程 2-后台改价父流程 3-后台改价子流程
     */
    private Integer           processType;

    /**
     * 模版id
     */
    private Integer           templateId;

    /**
     * 关联业务唯一No. -1-无关联流程编号
     */
    private Long              refUniqueNo;

    /**
     * 关联父流程业务唯一No. -1-无关联父流程
     */
    private Long              parentRefUniqueNo;

    /**
     * 当前状态 -1-未初始化
     */
    private Integer           currentStatus;

    /**
     * 最后操作人type, 流程操作人类型
     */
    private Integer           latestOperatorType;

    /**
     * 流程操作人id, 最后操作人id
     */
    private Long              latestOperatorId;

    /**
     * 流程操作人名称, 最后操作人name
     */
    private String            latestOperator;

    /**
     * 创建时间
     */
    private Timestamp         ctime;

    /**
     * 更新时间
     */
    private Timestamp         mtime;

    /**
     * 备注
     */
    private String            remark;

    public UniversalProcessExtPo() {
    }

    public UniversalProcessExtPo(Long id, Long processNo, Integer processType, Integer templateId,
                                 Long refUniqueNo, Long parentRefUniqueNo, Integer currentStatus,
                                 Integer latestOperatorType, Long latestOperatorId,
                                 String latestOperator, Timestamp ctime, Timestamp mtime,
                                 String remark) {
        this.id = id;
        this.processNo = processNo;
        this.processType = processType;
        this.templateId = templateId;
        this.refUniqueNo = refUniqueNo;
        this.parentRefUniqueNo = parentRefUniqueNo;
        this.currentStatus = currentStatus;
        this.latestOperatorType = latestOperatorType;
        this.latestOperatorId = latestOperatorId;
        this.latestOperator = latestOperator;
        this.ctime = ctime;
        this.mtime = mtime;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProcessNo() {
        return processNo;
    }

    public void setProcessNo(Long processNo) {
        this.processNo = processNo;
    }

    public Integer getProcessType() {
        return processType;
    }

    public void setProcessType(Integer processType) {
        this.processType = processType;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public Long getRefUniqueNo() {
        return refUniqueNo;
    }

    public void setRefUniqueNo(Long refUniqueNo) {
        this.refUniqueNo = refUniqueNo;
    }

    public Long getParentRefUniqueNo() {
        return parentRefUniqueNo;
    }

    public void setParentRefUniqueNo(Long parentRefUniqueNo) {
        this.parentRefUniqueNo = parentRefUniqueNo;
    }

    public Integer getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(Integer currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Integer getLatestOperatorType() {
        return latestOperatorType;
    }

    public void setLatestOperatorType(Integer latestOperatorType) {
        this.latestOperatorType = latestOperatorType;
    }

    public Long getLatestOperatorId() {
        return latestOperatorId;
    }

    public void setLatestOperatorId(Long latestOperatorId) {
        this.latestOperatorId = latestOperatorId;
    }

    public String getLatestOperator() {
        return latestOperator;
    }

    public void setLatestOperator(String latestOperator) {
        this.latestOperator = latestOperator;
    }

    public Timestamp getCtime() {
        return ctime;
    }

    public void setCtime(Timestamp ctime) {
        this.ctime = ctime;
    }

    public Timestamp getMtime() {
        return mtime;
    }

    public void setMtime(Timestamp mtime) {
        this.mtime = mtime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}