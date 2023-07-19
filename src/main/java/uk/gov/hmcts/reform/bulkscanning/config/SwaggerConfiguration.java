package uk.gov.hmcts.reform.bulkscanning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.GroupedOpenApi;


@Configuration
public class SwaggerConfiguration {

    private static final String HEADER = "header";

    @Bean
    public GroupedOpenApi api() {

        return GroupedOpenApi.builder()
            .group("bulkscan")
            .packagesToScan("uk.gov.hmcts.reform.bulkscanning.controller")
            .pathsToMatch("/**")
            .addOperationCustomizer(authorizationHeaders())
            .build();
    }
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().components(new Components())
            .info(new Info().title("Bulkscanning App").version("1.0.0"));
    }

    @Bean
    public OperationCustomizer authorizationHeaders() {
        return (operation, handlerMethod) ->
            operation
                .addParametersItem(
                    mandatoryStringParameter("Authorization", "User authorization header"))
                .addParametersItem(
                    mandatoryStringParameter("ServiceAuthorization", "Service authorization header"));
    }

    private Parameter mandatoryStringParameter(String name, String description) {
        return new Parameter()
            .name(name)
            .description(description)
            .required(true)
            .in(HEADER)
            .schema(new StringSchema());
    }
}
