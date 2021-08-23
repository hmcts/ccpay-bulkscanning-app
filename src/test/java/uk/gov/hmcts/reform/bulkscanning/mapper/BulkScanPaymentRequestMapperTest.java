package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BulkScanPaymentRequestMapperTest {

    @Test
    public void testMapEnvelopeFromBulkScanPaymentRequest_WithExceptionFalse() {
        BulkScanPaymentRequest bsPaymentRequest = BulkScanPaymentRequest.createBSPaymentRequestWith()
                                                        .ccdCaseNumber("ccd-number")
                                                        .isExceptionRecord(false)
                                                        .documentControlNumbers(new String[]{"dcn-number"})
                                                        .responsibleServiceId("AA07")
                                                        .build();
        BulkScanPaymentRequestMapper mapper = new BulkScanPaymentRequestMapper();
        Envelope  response = mapper.mapEnvelopeFromBulkScanPaymentRequest(bsPaymentRequest);
        assertEquals("INCOMPLETE",response.getPaymentStatus(),"Status should be INCOMPLETE");
    }

    @Test
    public void testMapEnvelopeFromBulkScanPaymentRequest_WithExceptionTrue() {
        BulkScanPaymentRequest bsPaymentRequest = BulkScanPaymentRequest.createBSPaymentRequestWith()
            .ccdCaseNumber("ccd-number")
            .isExceptionRecord(true)
            .documentControlNumbers(new String[]{"dcn-number"})
            .responsibleServiceId("AA07")
            .build();
        BulkScanPaymentRequestMapper mapper = new BulkScanPaymentRequestMapper();
        Envelope  response = mapper.mapEnvelopeFromBulkScanPaymentRequest(bsPaymentRequest);
        assertEquals("INCOMPLETE",response.getPaymentStatus(),"Status should be INCOMPLETE");
    }
}
