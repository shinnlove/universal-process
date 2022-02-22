/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.batch;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author Tony Zhao
 * @version $Id: BatchInitResult.java, v 0.1 2022-02-22 11:25 AM Tony Zhao Exp $$
 */
public class BatchInitResult implements Serializable {

    private static final long  serialVersionUID = 7625903817252744996L;

    /** parent ref unique no. */
    private long               parentRefUniqueNo;

    /** template id => ProcessContext */
    private Map<Integer, Long> processNos;

    public BatchInitResult() {
    }

    public BatchInitResult(long parentRefUniqueNo, Map<Integer, Long> processNos) {
        this.parentRefUniqueNo = parentRefUniqueNo;
        this.processNos = processNos;
    }

    public long getParentRefUniqueNo() {
        return parentRefUniqueNo;
    }

    public void setParentRefUniqueNo(long parentRefUniqueNo) {
        this.parentRefUniqueNo = parentRefUniqueNo;
    }

    public Map<Integer, Long> getProcessNos() {
        return processNos;
    }

    public void setProcessNos(Map<Integer, Long> processNos) {
        this.processNos = processNos;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}