package com.bilibili.universal.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.bilibili.universal.dao.model.UniversalProcessPo;
import com.bilibili.universal.dao.model.UniversalProcessPoExample;

public interface UniversalProcessDao {
    long countByExample(UniversalProcessPoExample example);

    int deleteByExample(UniversalProcessPoExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UniversalProcessPo record);

    int insertSelective(UniversalProcessPo record);

    List<UniversalProcessPo> selectByExample(UniversalProcessPoExample example);

    UniversalProcessPo selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") UniversalProcessPo record,
                                 @Param("example") UniversalProcessPoExample example);

    int updateByExample(@Param("record") UniversalProcessPo record,
                        @Param("example") UniversalProcessPoExample example);

    int updateByPrimaryKeySelective(UniversalProcessPo record);

    int updateByPrimaryKey(UniversalProcessPo record);
}