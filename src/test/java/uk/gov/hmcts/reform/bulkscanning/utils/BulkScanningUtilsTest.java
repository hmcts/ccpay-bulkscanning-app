package uk.gov.hmcts.reform.bulkscanning.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.mockBulkScanningEnvelope;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(locations="classpath:application-local.yaml")
public class BulkScanningUtilsTest {

    @Test
    public void testInsertStatusHistoryTest() {
        StatusHistory statusHistory = StatusHistory
            .statusHistoryWith().
                envelope(mockBulkScanningEnvelope()).
                 id(1).
                dateUpdated(LocalDateTime.now()).
                dateCreated(LocalDateTime.now()).
                build();

        Assert.assertNotNull(statusHistory);
    }
}
