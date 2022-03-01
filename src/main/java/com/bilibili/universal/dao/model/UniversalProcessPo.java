package com.bilibili.universal.dao.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class UniversalProcessPo implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 流程编号
     */
    private Long processNo;

    /**
     * 流程所属业务: 1-自助改价流程 2-后台改价父流程 3-后台改价子流程
     */
    private Integer processType;

    /**
     * 模版id
     */
    private Integer templateId;

    /**
     * 关联业务唯一No. -1-无关联流程编号
     */
    private Long refUniqueNo;

    /**
     * 关联父流程业务唯一No. -1-无关联父流程
     */
    private Long parentRefUniqueNo;

    /**
     * 当前状态 -1-未初始化
     */
    private Integer currentStatus;

    /**
     * 最后操作人type, 流程操作人类型
     */
    private Integer latestOperatorType;

    /**
     * 流程操作人id, 最后操作人id
     */
    private Long latestOperatorId;

    /**
     * 流程操作人名称, 最后操作人name
     */
    private String latestOperator;

    /**
     * 创建时间
     */
    private Timestamp ctime;

    /**
     * 更新时间
     */
    private Timestamp mtime;

    /**
     * 备注
     */
    private String remark;

    private static final long serialVersionUID = 1L;

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
        this.latestOperator = latestOperator == null ? null : latestOperator.trim();
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
        this.remark = remark == null ? null : remark.trim();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        UniversalProcessPo other = (UniversalProcessPo) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getProcessNo() == null ? other.getProcessNo() == null : this.getProcessNo().equals(other.getProcessNo()))
            && (this.getProcessType() == null ? other.getProcessType() == null : this.getProcessType().equals(other.getProcessType()))
            && (this.getTemplateId() == null ? other.getTemplateId() == null : this.getTemplateId().equals(other.getTemplateId()))
            && (this.getRefUniqueNo() == null ? other.getRefUniqueNo() == null : this.getRefUniqueNo().equals(other.getRefUniqueNo()))
            && (this.getParentRefUniqueNo() == null ? other.getParentRefUniqueNo() == null : this.getParentRefUniqueNo().equals(other.getParentRefUniqueNo()))
            && (this.getCurrentStatus() == null ? other.getCurrentStatus() == null : this.getCurrentStatus().equals(other.getCurrentStatus()))
            && (this.getLatestOperatorType() == null ? other.getLatestOperatorType() == null : this.getLatestOperatorType().equals(other.getLatestOperatorType()))
            && (this.getLatestOperatorId() == null ? other.getLatestOperatorId() == null : this.getLatestOperatorId().equals(other.getLatestOperatorId()))
            && (this.getLatestOperator() == null ? other.getLatestOperator() == null : this.getLatestOperator().equals(other.getLatestOperator()))
            && (this.getCtime() == null ? other.getCtime() == null : this.getCtime().equals(other.getCtime()))
            && (this.getMtime() == null ? other.getMtime() == null : this.getMtime().equals(other.getMtime()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getProcessNo() == null) ? 0 : getProcessNo().hashCode());
        result = prime * result + ((getProcessType() == null) ? 0 : getProcessType().hashCode());
        result = prime * result + ((getTemplateId() == null) ? 0 : getTemplateId().hashCode());
        result = prime * result + ((getRefUniqueNo() == null) ? 0 : getRefUniqueNo().hashCode());
        result = prime * result + ((getParentRefUniqueNo() == null) ? 0 : getParentRefUniqueNo().hashCode());
        result = prime * result + ((getCurrentStatus() == null) ? 0 : getCurrentStatus().hashCode());
        result = prime * result + ((getLatestOperatorType() == null) ? 0 : getLatestOperatorType().hashCode());
        result = prime * result + ((getLatestOperatorId() == null) ? 0 : getLatestOperatorId().hashCode());
        result = prime * result + ((getLatestOperator() == null) ? 0 : getLatestOperator().hashCode());
        result = prime * result + ((getCtime() == null) ? 0 : getCtime().hashCode());
        result = prime * result + ((getMtime() == null) ? 0 : getMtime().hashCode());
        result = prime * result + ((getRemark() == null) ? 0 : getRemark().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", processNo=").append(processNo);
        sb.append(", processType=").append(processType);
        sb.append(", templateId=").append(templateId);
        sb.append(", refUniqueNo=").append(refUniqueNo);
        sb.append(", parentRefUniqueNo=").append(parentRefUniqueNo);
        sb.append(", currentStatus=").append(currentStatus);
        sb.append(", latestOperatorType=").append(latestOperatorType);
        sb.append(", latestOperatorId=").append(latestOperatorId);
        sb.append(", latestOperator=").append(latestOperator);
        sb.append(", ctime=").append(ctime);
        sb.append(", mtime=").append(mtime);
        sb.append(", remark=").append(remark);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}