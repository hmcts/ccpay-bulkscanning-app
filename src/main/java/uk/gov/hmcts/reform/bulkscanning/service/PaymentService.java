package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.dto.StatusHistoryDto;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

public interface PaymentService {

    EnvelopePayment getPaymentByDcnReference(String dcnReference) throws PaymentException;

    EnvelopePayment updatePayment(EnvelopePayment payment) throws PaymentException;

    PaymentMetadata createPaymentMetadata(PaymentMetadataDto paymentMetadataDto) throws PaymentException;

    StatusHistory createStatusHistory(StatusHistoryDto statusHistoryDto) throws PaymentException;

    Envelope updateEnvelopePaymentStatus(Envelope envelope) throws PaymentException;

    Envelope createEnvelope(EnvelopeDto envelopeDto) throws PaymentException;

}
