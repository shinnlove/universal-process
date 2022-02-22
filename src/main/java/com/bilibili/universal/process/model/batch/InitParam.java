/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.batch;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.bilibili.universal.process.annotation.RequiredParam;
import com.bilibili.universal.process.model.context.DataContext;

/**
 * @author Tony Zhao
 * @version $Id: InitParam.java, v 0.1 2022-02-22 11:24 AM Tony Zhao Exp $$
 */
public class InitParam implements Serializable {

    private static final long serialVersionUID = -4923194334467690991L;

    @RequiredParam
    private int               templateId;

    @RequiredParam
    private long              refUniqueNo;

    @RequiredParam
    private DataContext       dataContext;

    public InitParam() {
    }

    public InitParam(int templateId, long refUniqueNo, DataContext dataContext) {
        this.templateId = templateId;
        this.refUniqueNo = refUniqueNo;
        this.dataContext = dataContext;
    }

    public int getTemplateId() {
        return templateId;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public long getRefUniqueNo() {
        return refUniqueNo;
    }

    public void setRefUniqueNo(long refUniqueNo) {
        this.refUniqueNo = refUniqueNo;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public void setDataContext(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}