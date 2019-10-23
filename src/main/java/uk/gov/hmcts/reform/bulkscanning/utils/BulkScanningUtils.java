package uk.gov.hmcts.reform.bulkscanning.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.exception.BulkScanCaseAlreadyExistsException;
import uk.gov.hmcts.reform.bulkscanning.exception.DcnNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.EnvelopeSource.Both;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST;

@Component
public class BulkScanningUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    PaymentRepository paymentRepository;

    public Envelope updatePaymentStatus(String dcn, PaymentStatus status) {
        if (Optional.ofNullable(dcn).isPresent()) {
            EnvelopePayment envelopePayment = paymentRepository.findByDcnReference(dcn).orElseThrow(DcnNotExistsException::new);
            envelopePayment.setPaymentStatus(status.toString());
            return envelopePayment.getEnvelope();
        }
        return null;
    }

    public List<Envelope> returnExistingEnvelopeList(Envelope envelope) {
        List<Envelope> existingEnvelopeList = new ArrayList<>();

        //List of existing Payments
        List<EnvelopePayment> listOfExistingPayment = envelope.getEnvelopePayments().stream().filter(envelopePayment ->
            paymentRepository.findByDcnReference(envelopePayment.getDcnReference()).isPresent())
            .map(envelopePayment -> paymentRepository.findByDcnReference(envelopePayment.getDcnReference()).get())
            .collect(Collectors.toList());

        if (Optional.ofNullable(listOfExistingPayment).isPresent() && !listOfExistingPayment.isEmpty()) {
            //Existing envelopeList
            existingEnvelopeList = listOfExistingPayment.stream().
                map(envelopePayment -> listOfExistingEnvelope(envelopePayment)).
                filter(envelope1 -> Optional.ofNullable(envelope1.getId()).isPresent()).
                collect(Collectors.toList());
        } else {
            //New Envelope
            existingEnvelopeList.add(envelope);
        }

        return existingEnvelopeList;
    }

    public Envelope listOfExistingEnvelope(EnvelopePayment envelopePayment) {
        Envelope existingEnvelope = null;

        if (Optional.ofNullable(envelopePayment).isPresent()) {
             existingEnvelope = envelopePayment.getEnvelope();

            //check if existing cases are present
            if (Optional.ofNullable(existingEnvelope).isPresent() &&
                Optional.ofNullable(existingEnvelope.getEnvelopeCases()).isPresent()
                && !existingEnvelope.getEnvelopeCases().isEmpty()) {
                throw new BulkScanCaseAlreadyExistsException(BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST);
            }

        }

        return existingEnvelope;
    }

    public boolean isDCNAlreadyExists(List<String> listOfExistingDCN, EnvelopePayment envelopePayment) {
        return !listOfExistingDCN.stream().noneMatch(dcn -> dcn.equalsIgnoreCase(envelopePayment.getDcnReference()));
    }

    public Envelope handlePaymentStatus(Envelope envelopeDB, Envelope envelopeNew) {

        processAllTheDCNPayments(envelopeDB,envelopeNew);
        completeTheEnvelopeStatus(envelopeDB);
        mergeTheCaseDetailsFromBulkScan(envelopeDB,envelopeNew);

        return envelopeDB;

    }

    public void mergeTheCaseDetailsFromBulkScan(Envelope envelopeDB, Envelope envelopeNew){
        envelopeDB.setResponsibleServiceId(envelopeNew.getResponsibleServiceId());
        List<EnvelopeCase> envelopeCaseList = new ArrayList<>(envelopeNew.getEnvelopeCases());

        envelopeDB.setEnvelopeCases(envelopeCaseList);
        envelopeDB.getEnvelopeCases().stream().forEach(envelopeCase -> envelopeCase.setEnvelope(envelopeDB));
    }

    public void processAllTheDCNPayments(Envelope envelopeDB, Envelope envelopeNew) {

        //List of existing DCN
        List<String> listOfExistingDCN = envelopeNew.getEnvelopePayments().stream().filter(envelopePayment ->
            paymentRepository.findByDcnReference(envelopePayment.getDcnReference()).isPresent())
            .map(envelopePayment -> paymentRepository.findByDcnReference(envelopePayment.getDcnReference()).get())
            .map(envelopePayment -> envelopePayment.getDcnReference()).collect(Collectors.toList());

        List<EnvelopePayment> listOfNewDCNs = envelopeNew.getEnvelopePayments().stream().filter(envelopePayment ->
            !(paymentRepository.findByDcnReference(envelopePayment.getDcnReference()).isPresent()))
            .collect(Collectors.toList());

        if (Optional.ofNullable(listOfExistingDCN).isPresent() && !listOfExistingDCN.isEmpty()) {
            //update DCN Status to complete if already present
            LOG.info("List of existing DCN payments: {}", listOfExistingDCN.size());
            envelopeDB.getEnvelopePayments().stream().filter(envelopePayment ->
                isDCNAlreadyExists(listOfExistingDCN, envelopePayment))
                .forEach(envelopePayment -> {
                    LOG.info("Updating Payment Source to BOTH as we have received payment from Bulk_Scan & Excela");
                    envelopePayment.setSource(Both.toString());
                    envelopePayment.setPaymentStatus(COMPLETE.toString());
                });
        }

        if (Optional.ofNullable(listOfNewDCNs).isPresent() && !listOfNewDCNs.isEmpty()) {
            //add new DCN list to existing envelope DB
            LOG.info("List of New DCN payments: {}", listOfNewDCNs.size());
            envelopeDB.getEnvelopePayments().addAll(listOfNewDCNs);
        }
    }

    public void completeTheEnvelopeStatus(Envelope envelopeDB) {

        //list of incomplete DCN
        List<EnvelopePayment> listOfIncompleteDCN = envelopeDB.getEnvelopePayments().stream()
            .filter(envelopePayment -> StringUtils.equalsIgnoreCase(envelopePayment.getPaymentStatus(),PaymentStatus.INCOMPLETE.toString()))
            .collect(Collectors.toList());

        //update envelope, and status history if all the dcns are present and complete
        if (!Optional.ofNullable(listOfIncompleteDCN).isPresent() || listOfIncompleteDCN.isEmpty()) {
            LOG.info("Marking Envelope complete");
            envelopeDB.setPaymentStatus(COMPLETE.toString()); // update envelope status to complete
        }

    }

    public Envelope insertStatusHistoryAudit(Envelope envelope) {
        String paymentStatus = null;

        if (Optional.ofNullable(envelope).isPresent()) {
            if (Optional.ofNullable(envelope.getPaymentStatus()).isPresent()) {
                paymentStatus =  envelope.getPaymentStatus();
            }

            StatusHistory statusHistory = StatusHistory
                .statusHistoryWith()
                .envelope(envelope)
                .status(paymentStatus) //update Status History if envelope status
                .build();

            List<StatusHistory> statusHistoryList = new ArrayList<>();

            //If Status histories audit already present
            if (Optional.ofNullable(envelope.getStatusHistories()).isPresent()) {
                statusHistoryList.addAll(envelope.getStatusHistories());
            }
            statusHistoryList.add(statusHistory);
            envelope.setStatusHistories(statusHistoryList);
        }

        return envelope;
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
