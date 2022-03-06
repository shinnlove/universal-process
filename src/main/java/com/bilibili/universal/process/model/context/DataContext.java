/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.context;

import static com.bilibili.universal.process.consts.MachineConstant.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.bilibili.universal.process.interfaces.ActionHandler;

/**
 * A context hold process entrance data to do business.<br>
 * The original purpose is to prepare the process data before status machine doing all business,
 * so that it could avoid couples of handlers duplicate querying business data back and forth.<br><br>
 *
 * First define your data class with several fields, i.e.: RecruitMissionData.java<br>
 * Then use this class to hold your generic data class, i.e.: DataContext<RecruitMissionData> dataContext,
 * And thus you could proceed a new process smoothly like this:<br><br>
 *
 * statusMachineService.proceedProcess(actionId, refUniqueNo, dataContext);<br><br>
 *
 * Wonderful and Congratulation :)<br><br>
 *
 * @author Tony Zhao
 * @version $Id: DataContext.java, v 0.1 2021-07-22 11:53 AM Tony Zhao Exp $$
 */
public class DataContext<T> implements Serializable {
    private static final long         serialVersionUID    = -2019461634360608165L;

    /** give outta system a change to diy ref unique no */
    private long                      refUniqueNo         = -1;

    /** generic data passed by business code */
    private T                         data;

    /** An generic object for business handler to put data in it. A list of handler execute result hold by context! */
    @Deprecated
    private Map<String, Object>       handlerResult       = new HashMap<>();

    /** the process proceed operator type */
    private int                       operatorType        = DEFAULT_OPERATOR_TYPE;

    /** the process proceed operator id */
    private long                      operatorId          = DEFAULT_OPERATOR_ID;

    /** the process proceed operator and default is system. */
    private String                    operator            = DEFAULT_OPERATOR;

    /** The remark each process need to comment. */
    private String                    remark              = DEFAULT_REMARK;

    /** if parent process, holds children data context. template id => data context */
    private Map<Integer, DataContext> childrenDataContext = new HashMap<>();

    /**
     * Constructor for reflect.
     */
    public DataContext() {
    }

    /**
     * Constructor with data arguments.
     *
     * @param data
     */
    public DataContext(T data) {
        this.data = data;
    }

    /**
     * Constructor for all arguments.
     * 
     * @param data 
     * @param handlerResult
     * @param operatorId
     * @param operator
     * @param remark
     */
    public DataContext(T data, Map<String, Object> handlerResult, long operatorId, String operator,
                       String remark) {
        this.data = data;
        this.handlerResult = handlerResult;
        this.operatorId = operatorId;
        this.operator = operator;
        this.remark = remark;
    }

    public long getRefUniqueNo() {
        return refUniqueNo;
    }

    public void setRefUniqueNo(long refUniqueNo) {
        this.refUniqueNo = refUniqueNo;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Map<String, Object> getHandlerResult() {
        return handlerResult;
    }

    public void setHandlerResult(Map<String, Object> handlerResult) {
        this.handlerResult = handlerResult;
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

    public Map<Integer, DataContext> getChildrenDataContext() {
        return childrenDataContext;
    }

    public void setChildren(int templateId, DataContext dataContext) {
        if (!childrenDataContext.containsKey(templateId)) {
            childrenDataContext.put(templateId, dataContext);
        }
    }

    /**
     * By given a handler name return the handler execute result.
     * Return null if current handler is pioneer of the given handler name!!
     *
     * @param clazz
     * @return
     */
    public Object getHandlerResultByName(Class<? extends ActionHandler> clazz) {
        String className = clazz.getName();
        if (handlerResult.containsKey(className)) {
            return handlerResult.get(className);
        }
        return null;
    }

    /**
     * Store handler execute result to a target position.
     *
     * @param clazz         the given class type
     * @param result        the execute result
     */
    @Deprecated
    public void storeHandlerResult(Class<? extends ActionHandler> clazz, Object result) {
        String className = clazz.getName();
        handlerResult.put(className, result);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}