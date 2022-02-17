/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service;

import java.util.List;

import com.bilibili.universal.process.interfaces.ActionHandler;
import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * This is an executor of action, each action step will call proceedAction once.
 *
 * @author Tony Zhao
 * @version $Id: ActionExecutor.java, v 0.1 2021-07-06 5:35 PM Tony Zhao Exp $$
 */
public interface ActionExecutor {

    /**
     * Entrance for execute an action in one process.
     *
     * @param context               process context
     * @param actionHandlers        action flows passed into process
     */
    void proceed(final ProcessContext context, List<ActionHandler> actionHandlers);

}
