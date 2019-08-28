package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.StatusHistoryDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.*;
import uk.gov.hmcts.reform.bulkscanning.model.request.SearchRequest;

import java.util.List;

public interface PaymentService {

    EnvelopePayment getPaymentByDcnReference(String dcnReference);

    EnvelopePayment updatePayment(EnvelopePayment payment);

    PaymentMetadata createPaymentMetadata(PaymentMetadataDto paymentMetadataDto);

    PaymentMetadata getPaymentMetadata(String dcnReference);

    StatusHistory createStatusHistory(StatusHistoryDto statusHistoryDto);

    Envelope updateEnvelopePaymentStatus(Envelope envelope);

    Envelope createEnvelope(EnvelopeDto envelopeDto);

    List<EnvelopeCase> getEnvelopeCaseByCCDReference(SearchRequest searchRequest);

    EnvelopeCase getEnvelopeCaseByDCN(SearchRequest searchRequest);

}
