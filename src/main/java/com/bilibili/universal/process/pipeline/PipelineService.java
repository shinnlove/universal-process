/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.pipeline;

/**
 * @author Tony Zhao
 * @version $Id: PipelineService.java, v 0.1 2021-12-28 11:19 PM Tony Zhao Exp $$
 */
public interface PipelineService {

    /**
     * @param actionId 
     * @return
     */
    Object doPipeline(int actionId);

}
