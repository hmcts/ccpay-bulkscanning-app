package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.dto.BSPaymentRequest;

public interface BSConsumerService {
    void saveInitialMetadataFromBS(BSPaymentRequest bsPaymentRequest);
}
