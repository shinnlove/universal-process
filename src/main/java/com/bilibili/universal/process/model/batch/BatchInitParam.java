/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.batch;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.bilibili.universal.process.annotation.OptionalParam;
import com.bilibili.universal.process.annotation.RequiredParam;
import com.bilibili.universal.process.model.context.DataContext;

/**
 * @author Tony Zhao
 * @version $Id: BatchInitParam.java, v 0.1 2022-02-22 11:40 AM Tony Zhao Exp $$
 */
public class BatchInitParam implements Serializable {

    private static final long serialVersionUID  = -4207239358374614450L;

    /** parent ref unique No. */
    @OptionalParam
    private long              parentRefUniqueNo = -1;

    @RequiredParam
    private DataContext       parentDataContext = new DataContext();

    @RequiredParam
    private List<InitParam>   params;

    /**
     * Constructor for reflect.
     */
    public BatchInitParam() {
    }

    /**
     * Constructor for single parameter.
     * 
     * @param params 
     */
    public BatchInitParam(List<InitParam> params) {
        this.params = params;
    }

    public BatchInitParam(long parentRefUniqueNo, DataContext parentDataContext,
                          List<InitParam> params) {
        this.parentRefUniqueNo = parentRefUniqueNo;
        this.parentDataContext = parentDataContext;
        this.params = params;
    }

    public long getParentRefUniqueNo() {
        return parentRefUniqueNo;
    }

    public void setParentRefUniqueNo(long parentRefUniqueNo) {
        this.parentRefUniqueNo = parentRefUniqueNo;
    }

    public DataContext getParentDataContext() {
        return parentDataContext;
    }

    public void setParentDataContext(DataContext parentDataContext) {
        this.parentDataContext = parentDataContext;
    }

    public List<InitParam> getParams() {
        return params;
    }

    public void setParams(List<InitParam> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}