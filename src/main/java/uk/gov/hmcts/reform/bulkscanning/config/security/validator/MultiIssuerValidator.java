package uk.gov.hmcts.reform.bulkscanning.config.security.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

import java.util.List;

public class MultiIssuerValidator implements OAuth2TokenValidator<Jwt> {
    private final Logger log = LoggerFactory.getLogger(MultiIssuerValidator.class);
    private final OAuth2Error error = new OAuth2Error("invalid_token",
                                                      "The required issuer is missing or invalid", null);

    private final List<String> validIssuers;

    public MultiIssuerValidator(List<String> validIssuers) {
        Assert.notEmpty(validIssuers, "Valid issuers should not be null or empty.");
        this.validIssuers = validIssuers;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        if (issuer != null && validIssuers.contains(issuer)) {
            return OAuth2TokenValidatorResult.success();
        } else {
            log.warn("Invalid issuer: {}", issuer);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
