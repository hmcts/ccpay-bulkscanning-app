package uk.gov.hmcts.reform.bulkscanning.config.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import uk.gov.hmcts.reform.bulkscanning.config.security.authcheckerconfiguration.AuthCheckerConfiguration;


import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class,classes = AuthCheckerConfiguration.class)
public class AuthCheckerConfigurationTest {

    @Autowired
    Function<HttpServletRequest, Optional<String>> userIdExtractor;

    @Autowired
    Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor;

    @Autowired
    Function<HttpServletRequest, Collection<String>> authorizedServicesExtractor;

    @Test
    public void testUserIdExtractor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("www.example.com");
        request.setRequestURI("/users/test/test1");
        request.setQueryString("param1=value1&param");
        Optional<String> value = userIdExtractor.apply(request);
        assertThat(value.get()).isEqualTo("test");
    }

    @Test
    public void testAuthorizedRolesExtractor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("www.example.com");
        request.setRequestURI("/users/test1/");
        request.setQueryString("param1=value1&param");
        Collection<String> value = authorizedRolesExtractor.apply(request);
        assertFalse(value.isEmpty(),"Function return an empty list");
    }

    @Test
    public void testAuthorizedServicesExtractor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("www.example.com");
        request.setRequestURI("/users/test1/");
        request.setQueryString("param1=value1&param");
        Collection<String> value = authorizedServicesExtractor.apply(request);
        assertFalse(value.isEmpty(),"Services are empty");
    }

}
