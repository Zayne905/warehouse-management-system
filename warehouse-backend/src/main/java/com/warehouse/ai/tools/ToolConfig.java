package com.warehouse.ai.tools;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that creates the ToolRegistry bean.
 * Tool classes auto-register via @PostConstruct.
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolRegistry toolRegistry() {
        return new ToolRegistry();
    }
}
