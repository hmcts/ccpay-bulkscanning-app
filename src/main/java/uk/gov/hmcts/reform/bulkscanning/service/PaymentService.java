package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentMetadataDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.StatusHistoryDTO;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

public interface PaymentService {

    EnvelopePayment getPaymentByDcnReference(String dcnReference) throws PaymentException;

    EnvelopePayment updatePayment(EnvelopePayment payment) throws PaymentException;

    PaymentMetadata createPaymentMetadata(PaymentMetadataDTO paymentMetadataDto) throws PaymentException;

    StatusHistory createStatusHistory(StatusHistoryDTO statusHistoryDto) throws PaymentException;

    Envelope updateEnvelopePaymentStatus(Envelope envelope) throws PaymentException;

    Envelope createEnvelope(EnvelopeDTO envelopeDto) throws PaymentException;

}
