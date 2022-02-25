/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.status;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A simple class for slowest status and its template id mapping.
 * 
 * @author Tony Zhao
 * @version $Id: DragStatusId.java, v 0.1 2022-02-25 6:32 PM Tony Zhao Exp $$
 */
public class DragStatusId implements Serializable {

    private static final long serialVersionUID = 7898658862038081151L;

    /** current status */
    private int               status;

    /** current template id */
    private int               tid;

    /**
     * Constructor for reflect.
     */
    public DragStatusId() {
    }

    public DragStatusId(int status, int tid) {
        this.status = status;
        this.tid = tid;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}