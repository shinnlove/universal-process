/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.dao.ext;

import org.apache.ibatis.annotations.Param;

import com.bilibili.universal.dao.model.UniversalProcessExtPo;

/**
 * The extension dao interface for universal process.
 *
 * @author Tony Zhao
 * @version $Id: UniversalProcessExtDao.java, v 0.1 2021-07-15 11:52 AM Tony Zhao Exp $$
 */
public interface UniversalProcessExtDao {

    /**
     * manually extension for select by process no to get and lock process.
     *
     * @param processNo        the unique process no generated by snowflake algorithm
     * @return
     */
    UniversalProcessExtPo selectByProcessNoForUpdate(@Param("process_no") Long processNo);

    /**
     * manually extension for select by process ref id to get and lock process.
     *
     * @param refUniqueNo       the ref unique No. for an external business.
     * @return
     */
    UniversalProcessExtPo selectByRefUniqueNoForUpdate(@Param("ref_unique_no") Long refUniqueNo);

}
