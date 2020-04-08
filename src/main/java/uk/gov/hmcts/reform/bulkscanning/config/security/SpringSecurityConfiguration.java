package uk.gov.hmcts.reform.bulkscanning.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.config.security.converter.BSJwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.bulkscanning.config.security.filiters.ServiceAndUserAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.config.security.utils.SecurityUtils;
import uk.gov.hmcts.reform.bulkscanning.config.security.validator.AudienceValidator;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Spring security configuration for s2s authorisation and user authentication
 */
@EnableWebSecurity (debug = true)
@Configuration
public class SpringSecurityConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityConfiguration.class);
    private static final String AUTHORISED_ROLE_PAYMENT = "payments";
    private static final String AUTHORISED_ROLE_CITIZEN = "citizen";

    @Configuration
    @Order(1)
    public static class ExternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        private final ServiceAuthFilter serviceAuthFilter;

        @Autowired
        public ExternalApiSecurityConfigurationAdapter(final ServiceAuthFilter serviceAuthFilter) {
            super();
            this.serviceAuthFilter = serviceAuthFilter;
        }

        @Override
        protected void configure(HttpSecurity http) {
            try {

                http.addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
                    .sessionManagement().sessionCreationPolicy(STATELESS).and()
                    .csrf().disable()
                    .formLogin().disable()
                    .logout().disable()
                    .requestMatchers()
                    .antMatchers(HttpMethod.POST, "/bulk-scan-payment")
                    .antMatchers(HttpMethod.POST, "/bulk-scan-payments")
                    .antMatchers(HttpMethod.PUT, "/bulk-scan-payments")
                    .and()
                    .authorizeRequests()
                    .anyRequest().anonymous().anyRequest().authenticated();

            } catch (Exception e) {
                LOG.info("Error in ExternalApiSecurityConfigurationAdapter: {}", e);
            }
        }
    }

    @Configuration
    @Order(2)
    public static class InternalApiSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
        private String issuerUri;

        @Value("${oidc.audience-list}")
        private String[] allowedAudiences;

        @Value("${oidc.issuer}")
        private String issuerOverride;



        private static final Logger LOG = LoggerFactory.getLogger(SpringSecurityConfiguration.class);
        private final ServiceAuthFilter serviceAuthFilter;
        private ServiceAndUserAuthFilter serviceAndUserAuthFilter;
        private JwtAuthenticationConverter jwtAuthenticationConverter;

        @Inject
        public InternalApiSecurityConfigurationAdapter(final BSJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
                                           final ServiceAuthFilter serviceAuthFilter,
                                           final Function<HttpServletRequest, Optional<String>> userIdExtractor,
                                           final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
                                           final SecurityUtils securityUtils) {
            super();
            this.serviceAndUserAuthFilter = new ServiceAndUserAuthFilter(
                userIdExtractor, authorizedRolesExtractor, securityUtils);
            this.serviceAuthFilter = serviceAuthFilter;
            jwtAuthenticationConverter = new JwtAuthenticationConverter();
            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
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
        protected void configure(HttpSecurity http) {
            try {
                http.addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
                    .addFilterAfter(serviceAndUserAuthFilter, BearerTokenAuthenticationFilter.class)
                    .sessionManagement().sessionCreationPolicy(STATELESS).and()
                    .csrf().disable()
                    .formLogin().disable()
                    .logout().disable()
                    .authorizeRequests()
                    .antMatchers(HttpMethod.PATCH, "/bulk-scan-payments/*").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                    .antMatchers(HttpMethod.GET, "/cases").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                    .antMatchers(HttpMethod.GET, "/cases/*").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                    .antMatchers(HttpMethod.GET, "/report/data").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                    .antMatchers(HttpMethod.GET, "/report/download").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                    .antMatchers(HttpMethod.GET, "/api/**").permitAll()
                    .antMatchers("/error").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .oauth2ResourceServer()
                    .jwt()
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                    .and()
                    .and()
                    .oauth2Client();

            } catch (Exception e) {
                LOG.info("Error in InternalApiSecurityConfigurationAdapter: {}", e);
            }
        }

        @Bean
        @SuppressWarnings("unchecked")
        JwtDecoder jwtDecoder() {
            NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
                JwtDecoders.fromOidcIssuerLocation(issuerUri);

            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(Arrays.asList(allowedAudiences));
            // We are using issuerOverride instead of issuerUri as SIDAM has the wrong issuer at the moment
            OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
            OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerOverride);
            OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withTimestamp,
                                                                                          withIssuer,
                                                                                          audienceValidator);
            jwtDecoder.setJwtValidator(withAudience);

            return jwtDecoder;
        }
    }

}

