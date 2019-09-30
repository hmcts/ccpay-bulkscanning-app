package uk.gov.hmcts.reform.bulkscanning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.auth.checker.core.SubjectResolver;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.bulkscanning.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.reform.bulkscanning.backdoors.UserResolverBackdoor;

@Configuration
public class ComponentTestConfiguration {
    @Bean
    public SubjectResolver<Service> serviceResolver() {
        return new ServiceResolverBackdoor();
    }

    @Bean
    public SubjectResolver<User> userResolver() {
        return new UserResolverBackdoor();
    }

}
