package uk.gov.hmcts.reform.bulkscanning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.GroupedOpenApi;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi api() {

        return GroupedOpenApi.builder()
            .group("payment")
            .packagesToScan("uk.gov.hmcts.reform.bulkscanning.controller")
            .pathsToMatch("/**")
            .build();
    }
}
