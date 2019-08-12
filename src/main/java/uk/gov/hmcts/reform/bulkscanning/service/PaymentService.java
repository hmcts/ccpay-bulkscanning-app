package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentMetadataDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.StatusHistoryDTO;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

public interface PaymentService {

    Payment getPaymentByDcnReference(String dcnReference);

    Payment createPayment(PaymentDTO paymentDto);

    Payment updatePayment(Payment payment);

    PaymentMetadata createPaymentMetadata(PaymentMetadataDTO paymentMetadataDto);

    StatusHistory createStatusHistory(StatusHistoryDTO statusHistoryDto);

    Envelope createEnvelope(EnvelopeDTO envelopeDto);
}
