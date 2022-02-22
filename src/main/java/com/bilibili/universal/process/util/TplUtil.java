/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.util;

import com.bilibili.universal.process.model.cache.TemplateCache;
import com.bilibili.universal.process.model.cache.TemplateMetadata;

/**
 * @author Tony Zhao
 * @version $Id: TplUtil.java, v 0.1 2022-02-22 2:15 PM Tony Zhao Exp $$
 */
public class TplUtil {

    public static int parentId(TemplateCache cache) {
        TemplateMetadata metadata = cache.getMetadata();
        return metadata.getParentId();
    }

}