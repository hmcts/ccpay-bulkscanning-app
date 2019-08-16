package uk.gov.hmcts.reform.bulkscanning.utils;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.exception.BulkScanCaseAlreadyExistsException;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.StatusHistoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BulkScanningUtils {

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    StatusHistoryRepository statusHistoryRepository;

    public Envelope returnExistingEnvelope(Envelope envelope) throws BulkScanCaseAlreadyExistsException {

        //List of existing Payments
        List<EnvelopePayment> listOfExistingPayment = envelope
            .getEnvelopePayments()
            .stream()
            .filter(envelopePayment -> paymentRepository.findByDcnReference(
                envelopePayment.getDcnReference()).isPresent())
            .map(envelopePayment -> paymentRepository.findByDcnReference(envelopePayment
                                                                             .getDcnReference()).get())
            .collect(Collectors.toList());

        if (Optional.ofNullable(listOfExistingPayment).isPresent() && listOfExistingPayment.size() > 0) {
            envelope = listOfExistingPayment.get(0).getEnvelope();

            //check if existing cases are present
            if (Optional.ofNullable(envelope.getEnvelopeCases()).isPresent() && envelope.getEnvelopeCases().size() > 0) {
                throw new BulkScanCaseAlreadyExistsException("Bulk Scanning case already exists");
            }
        }

        return envelope;
    }

    public void handlePaymentStatus(Envelope envelope) {

        //update DCN Status to complete if already present
        envelope.getEnvelopePayments().stream().filter(envelopePayment ->
                                                           paymentRepository.findByDcnReference(envelopePayment.getDcnReference())
                                                               .isPresent())
            .forEach(envelopePayment ->
                         envelopePayment.setPaymentStatus(PaymentStatus.COMPLETE.toString()));

        //list of incomplete Dcn
        List<EnvelopePayment> listOfIncompleteDcn = envelope.getEnvelopePayments().stream()
            .filter(envelopePayment -> StringUtils.equalsIgnoreCase(
                envelopePayment.getPaymentStatus(),
                PaymentStatus.INCOMPLETE.toString()
            ))
            .collect(Collectors.toList());

        //update envelope, and status history if all the dcns are present and complete
        if (!Optional.ofNullable(listOfIncompleteDcn).isPresent() || listOfIncompleteDcn.size() == 0) {
            envelope.setPaymentStatus(PaymentStatus.COMPLETE.toString()); // update envelope status to complete
        }

    }

    public Envelope insertStatusHistoryAudit(Envelope envelope) {

        StatusHistory statusHistory = StatusHistory
            .statusHistoryWith()
            .envelope(envelope)
            .status(envelope.getPaymentStatus()) //update Status History if envelope status
            .build();

        List<StatusHistory> statusHistoryList = new ArrayList<>();

        //If Status histories audit already present
        if (Optional.ofNullable(envelope.getStatusHistories()).isPresent()) {
            statusHistoryList = envelope.getStatusHistories();
        }
        statusHistoryList.add(statusHistory);
        envelope.setStatusHistories(statusHistoryList);

        return envelope;
    }
}
