/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service;

import com.bilibili.universal.process.callback.ProcessCallback;
import com.bilibili.universal.process.model.batch.BatchInitParam;
import com.bilibili.universal.process.model.batch.BatchInitResult;
import com.bilibili.universal.process.model.context.DataContext;

/**
 * @author Tony Zhao
 * @version $Id: StatusMachineBatchService.java, v 0.1 2022-02-22 11:23 AM Tony Zhao Exp $$
 */
public interface StatusMachineBatchService {

    BatchInitResult batchInitProcess(final BatchInitParam param);

    BatchInitResult batchInitProcess(final BatchInitParam param, final ProcessCallback callback);

    long proceedParentProcess(final int actionId, final long refUniqueNo,
                              final DataContext dataContext);

    long proceedParentProcess(final int actionId, final long refUniqueNo,
                              final DataContext dataContext, final ProcessCallback callback);

}
