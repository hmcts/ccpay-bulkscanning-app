package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.dto.BulkScanPaymentRequest;

public interface BulkScanConsumerService {
    void saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest);
}
