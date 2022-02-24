/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.cache;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Memory based status number and sort order holder.
 * 
 * @author Tony Zhao
 * @version $Id: StatusCache.java, v 0.1 2022-01-29 5:28 PM Tony Zhao Exp $$
 */
public class StatusCache implements Serializable, Comparable<StatusCache> {

    private static final long serialVersionUID = -5038079241955526372L;

    /** status unique number */
    private int               no               = -1;

    /** status sort order */
    private int               sequence         = -1;

    /** reflect parent status no */
    private int               ps               = -1;

    /** indicate whether status is normal accomplished */
    private int               accomplish       = 0;

    /** indicate whether status is initialize default status */
    private boolean           defaultDst       = false;

    /**
     * Constructor for reflect.
     */
    public StatusCache() {
    }

    /**
     * Constructor with basic parameters.
     * 
     * @param no 
     * @param sequence
     * @param ps
     */
    public StatusCache(int no, int sequence, int ps) {
        this.no = no;
        this.sequence = sequence;
        this.ps = ps;
    }

    /**
     * Constructor with all arguments.
     * 
     * @param no 
     * @param sequence
     * @param ps
     * @param accomplish
     * @param defaultDst
     */
    public StatusCache(int no, int sequence, int ps, int accomplish, boolean defaultDst) {
        this.no = no;
        this.sequence = sequence;
        this.ps = ps;
        this.accomplish = accomplish;
        this.defaultDst = defaultDst;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getPs() {
        return ps;
    }

    public void setPs(int ps) {
        this.ps = ps;
    }

    public int getAccomplish() {
        return accomplish;
    }

    public void setAccomplish(int accomplish) {
        this.accomplish = accomplish;
    }

    public boolean isDefaultDst() {
        return defaultDst;
    }

    public void setDefaultDst(boolean defaultDst) {
        this.defaultDst = defaultDst;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int compareTo(StatusCache o) {
        if (this.sequence < o.getSequence()) {
            return -1;
        } else if (this.sequence == o.getSequence()) {
            return 0;
        } else {
            return 1;
        }
    }

}