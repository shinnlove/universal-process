/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.autoconfig;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import com.bilibili.universal.process.consts.XmlParseConstant;
import com.bilibili.universal.util.log.LoggerUtil;

import javax.sql.DataSource;

/**
 * Universal process database autowire config bean.
 * 
 * @author Tony Zhao
 * @version $Id: DatabaseConfig.java, v 0.1 2022-02-17 3:34 PM Tony Zhao Exp $$
 */
@Configuration
@MapperScan(basePackages = { "com.bilibili.universal.dao" }, sqlSessionTemplateRef = "sqlSessionTemplate")
public class DatabaseConfig implements EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private Environment         environment;

    //    @Bean("primaryDataSource")
    //    @ConditionalOnMissingBean(name = "primaryDataSource")
    //    public DataSource dataSource() throws Exception {
    //        HikariConfig hikariConfig = new HikariConfig();
    //        hikariConfig.setPoolName("HikariCP Connection Pool");
    //        hikariConfig.setDataSourceClassName("com.mysql.cj.jdbc.Driver");
    //        hikariConfig.addDataSourceProperty("user", "root");
    //        hikariConfig.addDataSourceProperty("password", "");
    //        hikariConfig.addDataSourceProperty("url",
    //            "jdbc:mysql://127.0.0.1:3306/miffy?useUnicode=true&characterEncoding=UTF-8&tinyInt1isBit=false&allowMultiQueries=true&zeroDateTimeBehavior=convertToNull");
    //        hikariConfig.setMaximumPoolSize(15);
    //
    //        return new HikariDataSource(hikariConfig);
    //    }

    @Bean("sqlSessionFactory")
    @ConditionalOnMissingBean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("primaryDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(XmlParseConstant.MAPPER);

            Resource[] combination = new Resource[resources.length];
            System.arraycopy(resources, 0, combination, 0, resources.length);

            bean.setMapperLocations(combination);
            return bean.getObject();
        } catch (Exception e) {
            LoggerUtil.error(logger, e, "set mapper location error,", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Bean("sqlSessionTemplate")
    @ConditionalOnMissingBean(name = "sqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "transactionManager")
    @ConditionalOnMissingBean(name = "transactionManager")
    public DataSourceTransactionManager transactionManager(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "transactionTemplate")
    public TransactionTemplate transactionTemplate(@Qualifier("transactionManager") DataSourceTransactionManager dataSourceTransactionManager) {
        TransactionTemplate tx = new TransactionTemplate(dataSourceTransactionManager);
        // sql默认10s超时
        tx.setTimeout(10);
        return tx;
    }

    @Bean(name = "newTransactionTemplate")
    public TransactionTemplate newTransactionTemplate(@Qualifier("transactionManager") DataSourceTransactionManager dataSourceTransactionManager) {
        TransactionTemplate ntx = new TransactionTemplate(dataSourceTransactionManager);
        ntx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return ntx;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}