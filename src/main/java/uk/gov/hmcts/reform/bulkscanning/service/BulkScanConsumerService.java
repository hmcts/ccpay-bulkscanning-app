package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

public interface BulkScanConsumerService {
    String saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest);
}
