package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.dto.BulkScanPaymentRequest;

public interface BulkScanConsumerService {
    String saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest);
}
