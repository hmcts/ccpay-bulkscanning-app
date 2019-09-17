package uk.gov.hmcts.reform.bulkscanning.config.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserIdSupplier implements UserIdSupplier {
    @Override
    public String get() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getUsername();
    }
}
