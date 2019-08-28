package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.entity.*;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.ExelaPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;

public interface PaymentService {

    void processPaymentFromExela(ExelaPaymentRequest exelaPaymentRequest, String dcnReference);
    SearchResponse retrieveByCCDReference(String ccdReference);
    SearchResponse retrieveByDcn(String documentControlNumber);
    PaymentMetadata getPaymentMetadata(String dcnReference);
    Envelope saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest);
    String updateCaseReferenceForExceptionRecord (String exceptionRecordReference, CaseReferenceRequest caseReferenceRequest);
    String markPaymentAsProcessed(String dcn);

}
