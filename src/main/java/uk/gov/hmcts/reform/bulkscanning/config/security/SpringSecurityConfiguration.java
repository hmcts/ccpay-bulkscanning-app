package uk.gov.hmcts.reform.bulkscanning.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.serviceonly.AuthCheckerServiceOnlyFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
public class SpringSecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityConfiguration.class);

    @Configuration
    @Order(1)
    public static class ExternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private final AuthCheckerServiceOnlyFilter authCheckerServiceOnlyFilter;

        @Autowired
        public ExternalApiSecurityConfigurationAdapter(RequestAuthorizer<Service> serviceRequestAuthorizer,
                                                       AuthenticationManager authenticationManager) {
            super();
            authCheckerServiceOnlyFilter = new AuthCheckerServiceOnlyFilter(serviceRequestAuthorizer);
            authCheckerServiceOnlyFilter.setAuthenticationManager(authenticationManager);
        }

        @Override
        protected void configure(HttpSecurity http) {
            try {
                http
                    .requestMatchers()
                    .antMatchers(HttpMethod.POST, "/bulk-scan-payment")
                    .antMatchers(HttpMethod.POST, "/bulk-scan-payments")
                    .and()
                    .addFilter(authCheckerServiceOnlyFilter)
                    .csrf().disable()
                    .authorizeRequests()
                    .anyRequest().authenticated();
            } catch (Exception e) {
                LOG.info("Error in ExternalApiSecurityConfigurationAdapter: {}", e);
            }
        }
    }

    @Configuration
    @Order(2)
    public static class InternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private final AuthCheckerServiceAndAnonymousUserFilter authCheckerFilter;

        @Autowired
        public InternalApiSecurityConfigurationAdapter(RequestAuthorizer<User> userRequestAuthorizer,
                                                       RequestAuthorizer<Service> serviceRequestAuthorizer,
                                                       AuthenticationManager authenticationManager) {
            super();
            authCheckerFilter = new AuthCheckerServiceAndAnonymousUserFilter(serviceRequestAuthorizer, userRequestAuthorizer);
            authCheckerFilter.setAuthenticationManager(authenticationManager);
        }

        @Override
        public void configure(WebSecurity web) {
            web.ignoring().antMatchers("/swagger-ui.html",
                "/webjars/springfox-swagger-ui/**",
                "/swagger-resources/**",
                "/v2/**",
                "/refdata/**",
                "/health",
                "/health/liveness",
                "/info",
                "/favicon.ico",
                "/mock-api/**",
                "/");
        }

        @Override
        //@SuppressWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED", justification = "It's safe to disable CSRF protection as application is not being hit directly from the browser")
        protected void configure(HttpSecurity http) {
            try {
                http.addFilter(authCheckerFilter)
                    .sessionManagement().sessionCreationPolicy(STATELESS).and()
                    .csrf().disable()
                    .formLogin().disable()
                    .logout().disable()
                    .authorizeRequests()
                    .antMatchers(HttpMethod.PUT, "/bulk-scan-payments").hasAnyAuthority("payments")
                    .antMatchers(HttpMethod.PATCH, "/bulk-scan-payments/*").hasAnyAuthority("payments")
                    .antMatchers(HttpMethod.GET, "/cases").hasAnyAuthority("payments")
                    .antMatchers(HttpMethod.GET, "/cases/*").hasAnyAuthority("payments")
                    .antMatchers(HttpMethod.GET, "/api/**").permitAll()
                    .anyRequest().authenticated();
            } catch (Exception e) {
                LOG.info("Error in ExternalApiSecurityConfigurationAdapter: {}", e);
            }
        }
    }

}
