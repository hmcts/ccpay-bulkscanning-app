package uk.gov.hmcts.reform.bulkscanning.config;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;
import uk.gov.hmcts.reform.bulkscanning.config.security.authcheckerconfiguration.AuthCheckerConfiguration;
import uk.gov.hmcts.reform.bulkscanning.config.security.filiters.ServiceAndUserAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.config.security.idam.IdamRepository;
import uk.gov.hmcts.reform.bulkscanning.config.security.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@ActiveProfiles({"local", "test"})
public class SpringSecurityTestConfiguration {

    @Bean
    public ServiceAuthFilter serviceAuthFilter() {
        ServiceAuthorisationApi serviceAuthorisationApi = new ServiceAuthorisationApi() {
            @Override
            public String serviceToken(Map<String, String> signIn) {
                return null;
            }

            @Override
            public void authorise(String authHeader, String[] roles) {

            }

            @Override
            public String getServiceName(String authHeader) {
                if (StringUtils.containsIgnoreCase(authHeader, "cmc")) {
                    return "cmc";
                } else {
                    return "invalid-service";
                }
            }
        };

        //Test Authorised services
        List<String> authorisedServices = new ArrayList<>();
        authorisedServices.add("cmc");

        ServiceAuthTokenValidator serviceAuthTokenValidator = new ServiceAuthTokenValidator(serviceAuthorisationApi);
        return new ServiceAuthFilter(serviceAuthTokenValidator, authorisedServices);
    }

    @Bean
    public IdamRepository idamRepository() {
        return Mockito.mock(IdamRepository.class);
    }

    @Bean
    SecurityUtils securityUtils() {
        AuthTokenGenerator authTokenGenerator = new AuthTokenGenerator() {
            @Override
            public String generate() {
                return "testToken";
            }
        };
        return new SecurityUtils(authTokenGenerator, idamRepository());
    }

    @Bean
    public ServiceAndUserAuthFilter serviceAndUserAuthFilter() {
        AuthCheckerConfiguration authCheckerConfiguration = new AuthCheckerConfiguration();

        return new ServiceAndUserAuthFilter(
            authCheckerConfiguration.userIdExtractor(),
            authCheckerConfiguration.authorizedRolesExtractor(),
            securityUtils()
        );
    }

}
