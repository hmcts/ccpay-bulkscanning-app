package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

public interface BulkScanConsumerService {
    Envelope saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest);
    void updateCaseReferenceForExceptionRecord (String exceptionRecordReference, CaseReferenceRequest caseReferenceRequest);
}
