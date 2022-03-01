/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.log;


import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Process status log domain model.
 *
 * @author Tony Zhao
 * @version $Id: ProcessStatusLog.java, v 0.1 2021-07-07 3:05 PM Tony Zhao Exp $$
 */
public class ProcessStatusLog implements Serializable {
    private static final long serialVersionUID = 8750756148087424050L;

    private long              processNo;

    private int               templateId;
    private int               actionId;

    private int               sourceStatus;
    private int               destinationStatus;

    private int               operatorType;
    private long              operatorId;
    private String            operator;

    private String            remark;

    /**
     * No arguments constructor.
     */
    public ProcessStatusLog() {
    }

    /**
     * process_id, source and destination constructor.
     *
     * @param processNo
     * @param sourceStatus
     * @param destinationStatus
     */
    public ProcessStatusLog(long processNo, int sourceStatus, int destinationStatus) {
        this.processNo = processNo;
        this.sourceStatus = sourceStatus;
        this.destinationStatus = destinationStatus;
    }

    /**
     * Constructor with all arguments.
     * 
     * @param processNo 
     * @param templateId
     * @param actionId
     * @param sourceStatus
     * @param destinationStatus
     * @param operatorType
     * @param operatorId
     * @param operator
     * @param remark
     */
    public ProcessStatusLog(long processNo, int templateId, int actionId, int sourceStatus,
                            int destinationStatus, int operatorType, long operatorId,
                            String operator, String remark) {
        this.processNo = processNo;
        this.templateId = templateId;
        this.actionId = actionId;
        this.sourceStatus = sourceStatus;
        this.destinationStatus = destinationStatus;
        this.operatorType = operatorType;
        this.operatorId = operatorId;
        this.operator = operator;
        this.remark = remark;
    }

    public long getProcessNo() {
        return processNo;
    }

    public void setProcessNo(long processNo) {
        this.processNo = processNo;
    }

    public int getTemplateId() {
        return templateId;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public int getActionId() {
        return actionId;
    }

    public void setActionId(int actionId) {
        this.actionId = actionId;
    }

    public int getSourceStatus() {
        return sourceStatus;
    }

    public void setSourceStatus(int sourceStatus) {
        this.sourceStatus = sourceStatus;
    }

    public int getDestinationStatus() {
        return destinationStatus;
    }

    public void setDestinationStatus(int destinationStatus) {
        this.destinationStatus = destinationStatus;
    }

    public int getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(int operatorType) {
        this.operatorType = operatorType;
    }

    public long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
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