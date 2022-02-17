package com.bilibili.universal.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.bilibili.universal.dao.model.ProcessBlockingPo;
import com.bilibili.universal.dao.model.ProcessBlockingPoExample;

public interface ProcessBlockingDao {
    long countByExample(ProcessBlockingPoExample example);

    int deleteByExample(ProcessBlockingPoExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProcessBlockingPo record);

    int insertSelective(ProcessBlockingPo record);

    List<ProcessBlockingPo> selectByExample(ProcessBlockingPoExample example);

    ProcessBlockingPo selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProcessBlockingPo record,
                                 @Param("example") ProcessBlockingPoExample example);

    int updateByExample(@Param("record") ProcessBlockingPo record,
                        @Param("example") ProcessBlockingPoExample example);

    int updateByPrimaryKeySelective(ProcessBlockingPo record);

    int updateByPrimaryKey(ProcessBlockingPo record);
}