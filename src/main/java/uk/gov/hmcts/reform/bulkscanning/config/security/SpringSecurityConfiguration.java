package uk.gov.hmcts.reform.bulkscanning.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.config.security.converter.BSJwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.bulkscanning.config.security.exception.BSAccessDeniedHandler;
import uk.gov.hmcts.reform.bulkscanning.config.security.exception.BSAuthenticationEntryPoint;
import uk.gov.hmcts.reform.bulkscanning.config.security.filiters.ServiceAndUserAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.config.security.utils.SecurityUtils;
import uk.gov.hmcts.reform.bulkscanning.config.security.validator.AudienceValidator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Spring security configuration for s2s authorisation and user authentication
 */
@EnableWebSecurity
@Configuration
public class SpringSecurityConfiguration {

    private static final String AUTHORISED_ROLE_PAYMENT = "payments";
    private static final String AUTHORISED_ROLE_CITIZEN = "citizen";

    private final ServiceAuthFilter serviceAuthFilter;
    private final BSAuthenticationEntryPoint bsAuthenticationEntryPoint;
    private final BSAccessDeniedHandler bsAccessDeniedHandler;
    private final ServiceAndUserAuthFilter serviceAndUserAuthFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${oidc.audience-list}")
    private String[] allowedAudiences;

    public SpringSecurityConfiguration(
        final ServiceAuthFilter serviceAuthFilter,
        final BSAuthenticationEntryPoint bsAuthenticationEntryPoint,
        final BSJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
        final Function<HttpServletRequest, Optional<String>> userIdExtractor,
        final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
        final SecurityUtils securityUtils,
        final BSAccessDeniedHandler bsAccessDeniedHandler
    ) {
        super();
        this.serviceAuthFilter = serviceAuthFilter;
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        this.serviceAndUserAuthFilter = new ServiceAndUserAuthFilter(
            userIdExtractor, authorizedRolesExtractor, securityUtils);

        this.bsAuthenticationEntryPoint = bsAuthenticationEntryPoint;
        this.bsAccessDeniedHandler = bsAccessDeniedHandler;
    }

    /**
     * Common security options for all security filter chains.
     * Note: this modifies the http object in place.
     * @param http HttpSecurity object that is being modified
     * @throws Exception
     */
    private void commonSecurityOptions(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(excHandling -> excHandling
                .accessDeniedHandler(bsAccessDeniedHandler)
                .authenticationEntryPoint(bsAuthenticationEntryPoint)
            );
    }

    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        commonSecurityOptions(http);
        http
            .securityMatcher(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/health",
                "/health/liveness",
                "/health/readiness",
                "/mock-api/**",
                "/")
            .anonymous(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain serviceEndpointsFilterChain(HttpSecurity http) throws Exception {
        commonSecurityOptions(http);
        http
            .securityMatchers(matchers -> matchers
                .requestMatchers(HttpMethod.POST, "/bulk-scan-payment")
                .requestMatchers(HttpMethod.POST, "/bulk-scan-payments")
                .requestMatchers(HttpMethod.PUT, "/bulk-scan-payments")
                .requestMatchers(HttpMethod.GET, "/case/**"))
            .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain privateEndpointsFilterChain(HttpSecurity http) throws Exception {
        commonSecurityOptions(http);
        http
            .addFilterAfter(serviceAndUserAuthFilter, BearerTokenAuthenticationFilter.class)
            .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.PATCH, "/bulk-scan-payments/**").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                .requestMatchers(HttpMethod.GET, "/cases/**").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                .requestMatchers(HttpMethod.GET, "/report/data").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                .requestMatchers(HttpMethod.GET, "/report/download").hasAnyAuthority(AUTHORISED_ROLE_PAYMENT, AUTHORISED_ROLE_CITIZEN)
                .requestMatchers(HttpMethod.DELETE, "/bulk-scan-payment/**").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
            JwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(Arrays.asList(allowedAudiences));

        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();

        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(
            withTimestamp, audienceValidator);
        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }
}
