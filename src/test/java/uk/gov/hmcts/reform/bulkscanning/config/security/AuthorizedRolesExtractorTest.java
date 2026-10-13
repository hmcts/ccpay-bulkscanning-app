package uk.gov.hmcts.reform.bulkscanning.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.reform.bulkscanning.config.security.authcheckerconfiguration.AuthCheckerConfiguration;

import java.util.Collection;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizedRolesExtractorTest {

    private final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor =
        new AuthCheckerConfiguration().authorizedRolesExtractor();

    @Test
    public void shouldReturnPaymentsAndCitizenRolesForStandardPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/bulk-scan-payments");

        Collection<String> roles = authorizedRolesExtractor.apply(request);

        assertThat(roles).containsExactlyInAnyOrder("payments", "citizen");
    }

    @Test
    public void shouldReturnDisposerPaymentUserRoleForCaseBulkScanPaymentsDeletePath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("DELETE");
        request.setRequestURI("/cases/1747738470383168/bulk-scan-payments");

        Collection<String> roles = authorizedRolesExtractor.apply(request);

        assertThat(roles).containsExactly("disposer-payment-user");
    }

    @Test
    public void shouldReturnPaymentsAndCitizenRolesForGetCasesPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/cases/1747738470383168");

        Collection<String> roles = authorizedRolesExtractor.apply(request);

        assertThat(roles).containsExactlyInAnyOrder("payments", "citizen");
    }
}
