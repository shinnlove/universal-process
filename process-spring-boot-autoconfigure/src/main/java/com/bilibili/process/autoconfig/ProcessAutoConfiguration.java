/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.process.autoconfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bilibili.process.process.MyServiceImpl;

/**
 * @author Tony Zhao
 * @version $Id: ProcessAutoConfiguration.java, v 0.1 2022-02-16 3:03 PM Tony Zhao Exp $$
 */
@Configuration
@ConditionalOnProperty(prefix = "hello", value = "enabled", matchIfMissing = true)
public class ProcessAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MyServiceImpl.class)
    public MyServiceImpl helloService() {
        MyServiceImpl statusMachineService = new MyServiceImpl();
        return statusMachineService;
    }

}