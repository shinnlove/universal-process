/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.batch;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.bilibili.universal.process.annotation.OptionalParam;
import com.bilibili.universal.process.annotation.RequiredParam;
import com.bilibili.universal.process.model.context.DataContext;

/**
 * Each child process init param.
 * 
 * @author Tony Zhao
 * @version $Id: InitParam.java, v 0.1 2022-02-22 11:24 AM Tony Zhao Exp $$
 */
public class InitParam implements Serializable {

    private static final long serialVersionUID = -4923194334467690991L;

    /** template id for each child process */
    @RequiredParam
    private int               templateId       = -1;

    /** child process biz refer unique no */
    @RequiredParam
    private long              refUniqueNo;

    /** data context holds by child process. */
    @RequiredParam
    private DataContext       dataContext      = new DataContext();

    /** indicate process creation should route to which status number. */
    @OptionalParam
    private int               dst              = -1;

    public InitParam() {
    }

    public InitParam(int templateId, long refUniqueNo, DataContext dataContext) {
        this.templateId = templateId;
        this.refUniqueNo = refUniqueNo;
        this.dataContext = dataContext;
    }

    public InitParam(int templateId, long refUniqueNo, DataContext dataContext, int dst) {
        this.templateId = templateId;
        this.refUniqueNo = refUniqueNo;
        this.dataContext = dataContext;
        this.dst = dst;
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

    public int getDst() {
        return dst;
    }

    public void setDst(int dst) {
        this.dst = dst;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}