package uk.gov.hmcts.reform.bulkscanning.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentDeletionException;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.StatusHistoryRepository;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CasePaymentDeletionServiceImpl implements CasePaymentDeletionService {

    private static final Logger LOG = LoggerFactory.getLogger(CasePaymentDeletionServiceImpl.class);

    private final EnvelopeCaseRepository envelopeCaseRepository;
    private final EnvelopeRepository envelopeRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMetadataRepository paymentMetadataRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public CasePaymentDeletionServiceImpl(EnvelopeCaseRepository envelopeCaseRepository,
                                          EnvelopeRepository envelopeRepository,
                                          PaymentRepository paymentRepository,
                                          PaymentMetadataRepository paymentMetadataRepository,
                                          StatusHistoryRepository statusHistoryRepository) {
        this.envelopeCaseRepository = envelopeCaseRepository;
        this.envelopeRepository = envelopeRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Override
    @Transactional
    public void deleteByCcdCaseReference(String ccdCaseNumber) {
        try {
            List<EnvelopeCase> envelopeCases = envelopeCaseRepository.findByCcdReference(ccdCaseNumber)
                .orElse(Collections.emptyList());

            if (envelopeCases.isEmpty()) {
                LOG.info("No bulk scan payment records found for CCD case reference: {}. Returning success for idempotent behaviour.",
                    ccdCaseNumber);
                return;
            }

            Set<Integer> envelopeIds = envelopeCases.stream()
                .map(EnvelopeCase::getEnvelope)
                .filter(Objects::nonNull)
                .map(Envelope::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            for (Integer envelopeId : envelopeIds) {
                deleteEnvelopeRelatedRecords(envelopeId);
            }

            long deletedCases = envelopeCaseRepository.deleteByCcdReference(ccdCaseNumber);
            LOG.info("Deleted {} envelope_case record(s) for CCD case reference: {}", deletedCases, ccdCaseNumber);

            for (Integer envelopeId : envelopeIds) {
                envelopeRepository.deleteById(envelopeId);
                LOG.info("Deleted envelope with id: {} for CCD case reference: {}", envelopeId, ccdCaseNumber);
            }

            LOG.info("Successfully deleted bulk scan payment data for CCD case reference: {}", ccdCaseNumber);
        } catch (Exception ex) {
            LOG.error("Unexpected error occurred during deletion of bulk scan payments for CCD case reference: {}",
                ccdCaseNumber, ex);
            throw new PaymentDeletionException(
                "Unexpected error occurred during deletion for CCD case reference: " + ccdCaseNumber, ex);
        }
    }

    private void deleteEnvelopeRelatedRecords(Integer envelopeId) {
        long statusHistoryDeleted = statusHistoryRepository.deleteByEnvelopeId(envelopeId);
        LOG.info("Deleted {} status_history record(s) for envelope id: {}", statusHistoryDeleted, envelopeId);

        List<EnvelopePayment> payments = paymentRepository.findByEnvelopeId(envelopeId)
            .orElse(Collections.emptyList());

        List<String> dcnReferences = payments.stream()
            .map(EnvelopePayment::getDcnReference)
            .filter(Objects::nonNull)
            .toList();

        long metadataDeleted = paymentMetadataRepository.deleteByDcnReferenceIn(dcnReferences);
        LOG.info("Deleted {} payment_metadata record(s) for envelope id: {} and DCNs: {}",
            metadataDeleted, envelopeId, dcnReferences);

        long paymentsDeleted = paymentRepository.deleteByEnvelopeId(envelopeId);
        LOG.info("Deleted {} envelope_payment record(s) for envelope id: {}", paymentsDeleted, envelopeId);
    }
}
