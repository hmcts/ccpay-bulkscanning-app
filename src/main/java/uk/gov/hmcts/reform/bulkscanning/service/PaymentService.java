package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.StatusHistoryDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

public interface PaymentService {

    EnvelopePayment getPaymentByDcnReference(String dcnReference);

    EnvelopePayment updatePayment(EnvelopePayment payment);

    PaymentMetadata createPaymentMetadata(PaymentMetadataDto paymentMetadataDto);

    StatusHistory createStatusHistory(StatusHistoryDto statusHistoryDto);

    Envelope updateEnvelopePaymentStatus(Envelope envelope);

    Envelope createEnvelope(EnvelopeDto envelopeDto);

}
