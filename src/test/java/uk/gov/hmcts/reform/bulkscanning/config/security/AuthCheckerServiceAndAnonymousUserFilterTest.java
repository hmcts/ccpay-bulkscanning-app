package uk.gov.hmcts.reform.bulkscanning.config.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.reform.auth.checker.core.RequestAuthorizer;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.AuthCheckerException;
import uk.gov.hmcts.reform.auth.checker.core.exceptions.BearerTokenMissingException;
import uk.gov.hmcts.reform.auth.checker.core.service.Service;
import uk.gov.hmcts.reform.auth.checker.core.user.User;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserPair;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthCheckerServiceAndAnonymousUserFilterTest {

    @Mock
    RequestAuthorizer<Service> serviceRequestAuthorizer;

    @Mock
    RequestAuthorizer<User> userRequestAuthorizer;


    @InjectMocks
    AuthCheckerServiceAndAnonymousUserFilter filter;

    MockHttpServletRequest request;

    @Before
    public void setUp() {
        filter = new AuthCheckerServiceAndAnonymousUserFilter(serviceRequestAuthorizer, userRequestAuthorizer);
        request = new MockHttpServletRequest();
        request.setServerName("www.example.com");
        request.setRequestURI("/users/");
        request.addHeader("ServiceAuthorization", "dummyKey");
        request.addHeader("Authorization", "dummyAuthKey");
    }


    @Test
    public void testGetPreAuthenticatedPrincipal() {
        Service mockService = new Service("principalId");
        when(serviceRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(mockService);
        User mockUser = new User("principalId", null);
        when(userRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(mockUser);
        ServiceAndUserPair expectedResponse = new ServiceAndUserPair(mockService, mockUser);
        Object actualResponse = filter.getPreAuthenticatedPrincipal(request);
        assertThat(actualResponse).isEqualToComparingFieldByField(expectedResponse);
    }

    @Test
    public void testGetPreAuthenticatedPrincipal_WithServiceNull() {
        when(serviceRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(null);
        Object actualResponse = filter.getPreAuthenticatedPrincipal(request);
        assertNull(actualResponse,"Response should be null");
    }

    @Test
    public void testGetPreAuthenticatedPrincipal_WithUserNull() {
        Object actualResponse = filter.getPreAuthenticatedPrincipal(request);
        assertNull(actualResponse,"Response should be null");
    }

    @Test
    public void testGetPreAuthenticatedPrincipal_WithEmptyBearerToken() {
        Service mockService = new Service("principalId");
        when(serviceRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(mockService);
        when(userRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenThrow(BearerTokenMissingException.class);
        Set<String> anonymousRole = new HashSet<>(Arrays.asList("ROLE_ANONYMOUS"));
        User mockUser = new User("anonymous", anonymousRole);
        ServiceAndUserPair expectedResponse = new ServiceAndUserPair(mockService, mockUser);
        Object actualResponseObject = filter.getPreAuthenticatedPrincipal(request);
        ServiceAndUserPair actualResponse = (ServiceAndUserPair) actualResponseObject;
        assertIterableEquals(actualResponse.getUser().getRoles(), expectedResponse.getUser().getRoles());
    }

    @Test
    public void testGetPreAuthenticatedPrincipal_ThrowsAuthCheckerException() {
        Service mockService = new Service("principalId");
        when(serviceRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenReturn(mockService);
        when(userRequestAuthorizer.authorise(any(HttpServletRequest.class))).thenThrow(AuthCheckerException.class);
        Object actualResponseObject = filter.getPreAuthenticatedPrincipal(request);
        assertNull(actualResponseObject,"Response should be null");
    }


    @Test
    public void testGetPreAuthenticatedCredentials() {
        Object result = filter.getPreAuthenticatedCredentials(request);
        assertEquals("dummyAuthKey", result.toString(),"Credentials are invalid");
    }
}
