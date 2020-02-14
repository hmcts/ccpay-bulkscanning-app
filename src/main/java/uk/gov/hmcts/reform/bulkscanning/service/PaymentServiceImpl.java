package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.audit.AppInsightsAuditRepository;
import uk.gov.hmcts.reform.bulkscanning.exception.ExceptionRecordNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.mapper.BulkScanPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.EnvelopeSource.Both;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.*;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final PaymentMetadataRepository paymentMetadataRepository;

    private final EnvelopeRepository envelopeRepository;

    private final EnvelopeCaseRepository envelopeCaseRepository;

    private final AppInsightsAuditRepository auditRepository;

    private final PaymentMetadataDtoMapper paymentMetadataDtoMapper;

    private final EnvelopeDtoMapper envelopeDtoMapper;

    private final PaymentDtoMapper paymentDtoMapper;

    private final BulkScanPaymentRequestMapper bsPaymentRequestMapper;

    private final BulkScanningUtils bulkScanningUtils;

    private static final Logger LOG = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentMetadataRepository paymentMetadataRepository,
                              EnvelopeRepository envelopeRepository,
                              PaymentMetadataDtoMapper paymentMetadataDtoMapper,
                              EnvelopeDtoMapper envelopeDtoMapper,
                              PaymentDtoMapper paymentDtoMapper,
                              BulkScanPaymentRequestMapper bsPaymentRequestMapper,
                              BulkScanningUtils bulkScanningUtils,
                              EnvelopeCaseRepository envelopeCaseRepository,
                              AppInsightsAuditRepository auditRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.envelopeRepository = envelopeRepository;
        this.paymentMetadataDtoMapper = paymentMetadataDtoMapper;
        this.envelopeDtoMapper = envelopeDtoMapper;
        this.paymentDtoMapper = paymentDtoMapper;
        this.bsPaymentRequestMapper = bsPaymentRequestMapper;
        this.bulkScanningUtils = bulkScanningUtils;
        this.envelopeCaseRepository = envelopeCaseRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional
    public Envelope processPaymentFromExela(BulkScanPayment bulkScanPayment, String dcnReference) {
        LOG.info("Insert Payment metadata in Bulk Scan Payment DB");
        createPaymentMetadata(paymentMetadataDtoMapper.fromRequest(bulkScanPayment, dcnReference));

        LOG.info("Check for existing DCN in Payment Table Bulk Scan Pay DB");
        EnvelopePayment payment = getPaymentByDcnReference(dcnReference);

        if (null == payment) {
            LOG.info("Create new payment in BSP DB as envelope doesn't exists");
            List<PaymentDto> payments = new ArrayList<>();
            payments.add(paymentDtoMapper.fromRequest(bulkScanPayment, dcnReference));

            Envelope envelope = createEnvelope(EnvelopeDto.envelopeDtoWith()
                                                   .paymentStatus(INCOMPLETE)
                                                   .payments(payments)
                                                   .build());
            LOG.info("Envelope created with status as incomplete");
            auditRepository.trackPaymentEvent("EXELA_PAYMENT", envelope.getEnvelopePayments().get(0));
            return envelope;
        } else {
            if (Optional.ofNullable(payment.getEnvelope()).isPresent()
                && payment.getEnvelope().getPaymentStatus().equalsIgnoreCase(INCOMPLETE.toString())) {
                LOG.info("Update payment status as Complete");
                payment.setPaymentStatus(COMPLETE.toString());
                LOG.info("Updating Payment Source to BOTH as we have received payment from Bulk_Scan & Excela");
                payment.setSource(Both.toString());
                updatePayment(payment);
                auditRepository.trackPaymentEvent("EXELA_PAYMENT", payment);
                return updateEnvelopePaymentStatus(payment.getEnvelope(), COMPLETE);
            }
        }
        return null;
    }

    @Override
    @Transactional
    public List<String> saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest) {
        List<String> listOfAllPayments = new ArrayList<>();

        Envelope envelopeNew = bsPaymentRequestMapper.mapEnvelopeFromBulkScanPaymentRequest(bsPaymentRequest);
        List<Envelope> listOfExistingEnvelope = bulkScanningUtils.returnExistingEnvelopeList(envelopeNew);

        if (Optional.ofNullable(listOfExistingEnvelope).isPresent() && !listOfExistingEnvelope.isEmpty()) {
            for (Envelope envelopeDB : listOfExistingEnvelope) {
                //if we have envelope already in BS
                if (Optional.ofNullable(envelopeDB).isPresent() && Optional.ofNullable(envelopeDB.getId()).isPresent()) {
                    LOG.info("Existing envelope found for Bulk Scan request");
                    bulkScanningUtils.handlePaymentStatus(envelopeDB, envelopeNew);
                }

                bulkScanningUtils.insertStatusHistoryAudit(envelopeDB);
                envelopeRepository.save(envelopeDB);

                if (Optional.ofNullable(envelopeDB.getEnvelopePayments()).isPresent()
                    && !envelopeDB.getEnvelopePayments().isEmpty()) {
                    envelopeDB.getEnvelopePayments().stream().forEach(payment -> {
                        auditRepository.trackPaymentEvent("Bulk-Scan_PAYMENT", payment);
                    });
                }

                Optional<Envelope> envelope = envelopeRepository.findById(envelopeDB.getId());

                if(envelope.isPresent()) {
                    List<String> paymentDCNList = envelope.get().getEnvelopePayments().stream().map(envelopePayment -> envelopePayment.getDcnReference()).collect(
                        Collectors.toList());

                    listOfAllPayments.addAll(paymentDCNList);
                }

            }
        }

        if (Optional.ofNullable(listOfAllPayments).isPresent() && !listOfAllPayments.isEmpty()) {
            return listOfAllPayments;
        }
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public String updateCaseReferenceForExceptionRecord(String exceptionRecordReference, CaseReferenceRequest caseReferenceRequest) {
        List<EnvelopeCase> envelopeCases = envelopeCaseRepository.findByExceptionRecordReference(
            exceptionRecordReference).
            orElseThrow(ExceptionRecordNotExistsException::new);

        if (Optional.ofNullable(caseReferenceRequest).isPresent()
            && StringUtils.isNotEmpty(caseReferenceRequest.getCcdCaseNumber())
            && Optional.ofNullable(envelopeCases).isPresent()
            && !envelopeCases.isEmpty()) {
            envelopeCases.stream().forEach(envelopeCase -> {
                envelopeCase.setCcdReference(caseReferenceRequest.getCcdCaseNumber());
            });

            envelopeCaseRepository.saveAll(envelopeCases);
            return envelopeCases.stream().map(envelopeCase -> envelopeCase.getId().toString()).collect(Collectors.toList())
                .stream().collect(Collectors.joining(","));
        }

        return "";
    }

    @Override
    @Transactional
    public String updatePaymentStatus(String dcn, PaymentStatus status) {
        Envelope envelope = bulkScanningUtils.updatePaymentStatus(dcn, status);
        if (Optional.ofNullable(envelope).isPresent()) {
            envelopeRepository.save(envelope);
            updateEnvelopePaymentStatus(envelope, PROCESSED);
            if (Optional.ofNullable(envelope.getEnvelopePayments()).isPresent()
                && !envelope.getEnvelopePayments().isEmpty()) {
                envelope.getEnvelopePayments().stream().forEach(payment -> {
                    auditRepository.trackPaymentEvent("PAYMENT_STATUS_UPDATE", payment);
                });
            }
            return dcn;
        }
        return null;
    }

    @Override
    @Transactional
    public PaymentMetadata getPaymentMetadata(String dcnReference) {
        return paymentMetadataRepository.findByDcnReference(dcnReference).orElse(null);
    }

    private EnvelopePayment getPaymentByDcnReference(String dcnReference) {
        return paymentRepository.findByDcnReference(dcnReference).orElse(null);
    }

    private EnvelopePayment updatePayment(EnvelopePayment payment) {
        return paymentRepository.save(payment);
    }

    private PaymentMetadata createPaymentMetadata(PaymentMetadataDto paymentMetadataDto) {
        PaymentMetadata paymentMetadata = paymentMetadataDtoMapper.toPaymentEntity(paymentMetadataDto);
        return paymentMetadataRepository.save(paymentMetadata);
    }

    private Envelope updateEnvelopePaymentStatus(Envelope envelope, PaymentStatus paymentStatus) {
        List<EnvelopePayment> payments = paymentRepository.findByEnvelopeId(envelope.getId()).orElse(Collections.emptyList());
        if(null != payments && !payments.isEmpty()) {
            if (checkAllPaymentsStatus(paymentStatus, payments)) {
                updateEnvelopeStatus(envelope, paymentStatus);
            } else if (checkAnyPaymentsStatus(INCOMPLETE, payments)) {
                updateEnvelopeStatus(envelope, INCOMPLETE);
            } else if (checkAnyPaymentsStatus(COMPLETE, payments)) {
                updateEnvelopeStatus(envelope, COMPLETE);
            }
        }
        return envelope;
    }

    private boolean checkAllPaymentsStatus(PaymentStatus paymentStatus, List<EnvelopePayment> payments) {
        return Optional.ofNullable(payments).isPresent()
            && !payments.isEmpty()
            && payments.stream()
            .map(payment -> payment.getPaymentStatus())
            .collect(Collectors.toList())
            .stream().allMatch(paymentStatus.toString()::equals);
    }

    private boolean checkAnyPaymentsStatus(PaymentStatus paymentStatus, List<EnvelopePayment> payments) {
        return Optional.ofNullable(payments).isPresent()
            && !payments.isEmpty()
            && !payments.stream()
            .filter(payment -> payment.getPaymentStatus().equalsIgnoreCase(paymentStatus.toString()))
            .collect(Collectors.toList())
            .isEmpty();
    }

    private Envelope updateEnvelopeStatus(Envelope envelope, PaymentStatus paymentStatus) {
        envelope.setPaymentStatus(paymentStatus.toString());
        bulkScanningUtils.insertStatusHistoryAudit(envelope);
        return envelopeRepository.save(envelope);
    }

    private Envelope createEnvelope(EnvelopeDto envelopeDto) {
        Envelope envelope = envelopeRepository.save(envelopeDtoMapper.toEnvelopeEntity(envelopeDto));
        bulkScanningUtils.insertStatusHistoryAudit(envelope);
        return envelope;
    }
}
