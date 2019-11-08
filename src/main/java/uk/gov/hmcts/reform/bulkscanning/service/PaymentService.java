package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;

public interface PaymentService {

    Envelope processPaymentFromExela(BulkScanPayment bulkScanPayment, String dcnReference);
    PaymentMetadata getPaymentMetadata(String dcnReference);
    List<String> saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest);
    String updateCaseReferenceForExceptionRecord (String exceptionRecordReference, CaseReferenceRequest caseReferenceRequest);
    String updatePaymentStatus(String dcn, PaymentStatus status);
}
