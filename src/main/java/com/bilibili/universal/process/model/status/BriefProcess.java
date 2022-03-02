/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.status;

import java.io.Serializable;

/**
 * A simple class for brief cache with status, sequence and its template id mapping.
 * 
 * Utilize scenario: slowest child or deduce slowest child.
 * 
 * @author Tony Zhao
 * @version $Id: BriefProcess.java, v 0.1 2022-02-25 6:32 PM Tony Zhao Exp $$
 */
public class BriefProcess implements Serializable, Comparable<BriefProcess> {

    private static final long serialVersionUID = 7898658862038081151L;

    /** current template id */
    private int               tid;

    /** current sequence */
    private int               sequence;

    /** current status */
    private int               status;

    /**
     * Constructor for reflect.
     */
    public BriefProcess() {
    }

    public BriefProcess(int tid, int sequence, int status) {
        this.tid = tid;
        this.sequence = sequence;
        this.status = status;
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int compareTo(BriefProcess o) {
        if (this.sequence < o.getSequence()) {
            return -1;
        } else if (this.sequence == o.getSequence()) {
            return 0;
        } else {
            return 1;
        }
    }

}