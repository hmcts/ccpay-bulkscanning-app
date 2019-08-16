package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

public interface BulkScanConsumerService {
    void saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest);
}
