package uk.gov.hmcts.reform.bulkscanning.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import javax.inject.Inject;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity (debug = true)
@Configuration
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter{

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityConfiguration.class);
    private final ServiceAuthFilter serviceAuthFilter;
    private final AnonymousAuthenticationFilter anonymousAuthenticationFilter;

        @Inject
        public SpringSecurityConfiguration(final ServiceAuthFilter serviceAuthFilter, final AnonymousAuthenticationFilter anonymousAuthenticationFilter) {
            super();
            this.serviceAuthFilter =  serviceAuthFilter;
            this.anonymousAuthenticationFilter = anonymousAuthenticationFilter;
        }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/swagger-ui.html",
                                   "/webjars/springfox-swagger-ui/**",
                                   "/swagger-resources/**",
                                   "/v2/**",
                                   "/health",
                                   "/health/liveness",
                                   "/status/health",
                                   "/loggers/**",
                                   "/");
    }

        @Override
        protected void configure(HttpSecurity http) {
            try {
                http
                .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
                    .addFilter(new AnonymousAuthenticationFilter("anonymous"))
                .sessionManagement().sessionCreationPolicy(STATELESS)
                    .and().cors().and().csrf().disable().formLogin().disable()
                    .logout().disable()
                    .authorizeRequests().
                    antMatchers("/*").anonymous()
                    .anyRequest().authenticated();

            } catch (Exception e) {
                LOG.info("Error in ExternalApiSecurityConfigurationAdapter: {}", e);
            }
        }
    }
