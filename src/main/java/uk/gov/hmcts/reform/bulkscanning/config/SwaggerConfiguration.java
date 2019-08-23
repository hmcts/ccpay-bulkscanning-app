package uk.gov.hmcts.reform.bulkscanning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.hmcts.reform.bulkscanning.BulkScanningApiApplication;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    private List<Parameter> getGlobalOperationParameters() {
        return Arrays.asList(
            new ParameterBuilder()
                .name("ServiceAuthorization")
                .description("Service authorization header")
                .required(true)
                .parameterType("header")
                .modelRef(new ModelRef("string"))
                .build());
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .globalOperationParameters(getGlobalOperationParameters())
            .useDefaultResponseMessages(false)
            .apiInfo(apiInfo())
            .select()
            .apis(RequestHandlerSelectors.basePackage(BulkScanningApiApplication.class.getPackage()
                .getName() + ".controller"))
            .paths(PathSelectors.any())
            .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("Bulk Scannning API documentation")
            .description("Bulk Scanning API documentation")
            .build();
    }

}
