package uk.gov.hmcts.reform.bulkscanning.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.path.json.config.JsonPathConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.PostConstruct;

import static io.restassured.config.JsonConfig.jsonConfig;

@Configuration
@ComponentScan("uk.gov.hmcts.reform.bulkscanning")
@TestPropertySource(locations="classpath:application-unit-test.yaml")
public class TestContextConfiguration {

    @Value("${test.url:http://localhost:8080}")
    private String baseURL;

    @PostConstruct
    public void initialize() {
        RestAssured.config = RestAssured.config()
            .objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> new ObjectMapper())
            )
        .jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = baseURL;
    }

}
