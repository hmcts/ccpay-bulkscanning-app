package uk.gov.hmcts.reform.bulkscanning.utils;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.mockBulkScanningEnvelope;

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
