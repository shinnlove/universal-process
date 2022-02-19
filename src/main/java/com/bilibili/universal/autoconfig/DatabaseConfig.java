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
import org.springframework.transaction.support.TransactionTemplate;

import com.bilibili.universal.util.log.LoggerUtil;

import javax.sql.DataSource;

/**
 * @author Tony Zhao
 * @version $Id: DatabaseConfig.java, v 0.1 2022-02-17 3:34 PM Tony Zhao Exp $$
 */
@Configuration
@MapperScan(basePackages = { "com.bilibili.universal.dao" }, sqlSessionTemplateRef = "sqlSessionTemplate")
public class DatabaseConfig implements EnvironmentAware {

    private static final Logger logger     = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final String MAPPER     = "classpath*:META-INF/mapper/*.xml";

    private static final String EXT_MAPPER = "classpath*:META-INF/mapper/ext/*.xml";

    private Environment         environment;

    @Bean("sqlSessionFactory")
    @ConditionalOnMissingBean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("adReadDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(MAPPER);
            Resource[] extResources = resolver.getResources(EXT_MAPPER);

            Resource[] combination = new Resource[resources.length + extResources.length];
            System.arraycopy(resources, 0, combination, 0, resources.length);
            System.arraycopy(extResources, 0, combination, resources.length, extResources.length);

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
    public DataSourceTransactionManager transactionManager(@Qualifier("adReadDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "transactionTemplate")
    public TransactionTemplate transactionTemplate(@Qualifier("transactionManager") DataSourceTransactionManager dataSourceTransactionManager) {
        return new TransactionTemplate(dataSourceTransactionManager);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}