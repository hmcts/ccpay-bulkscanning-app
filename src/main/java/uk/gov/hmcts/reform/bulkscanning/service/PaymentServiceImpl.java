package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.exception.ExceptionRecordNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.mapper.*;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.*;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.repository.*;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.ExelaPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.SearchRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final PaymentMetadataRepository paymentMetadataRepository;

    private final EnvelopeRepository envelopeRepository;

    private final EnvelopeCaseRepository envelopeCaseRepository;

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
                              EnvelopeCaseRepository envelopeCaseRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.envelopeRepository = envelopeRepository;
        this.paymentMetadataDtoMapper = paymentMetadataDtoMapper;
        this.envelopeDtoMapper = envelopeDtoMapper;
        this.paymentDtoMapper = paymentDtoMapper;
        this.bsPaymentRequestMapper = bsPaymentRequestMapper;
        this.bulkScanningUtils = bulkScanningUtils;
        this.envelopeCaseRepository = envelopeCaseRepository;
    }

    @Override
    @Transactional
    public Envelope processPaymentFromExela(ExelaPaymentRequest exelaPaymentRequest, String dcnReference) {
        LOG.info("Insert Payment metadata in Bulk Scan Payment DB");//
        createPaymentMetadata(paymentMetadataDtoMapper.fromRequest(exelaPaymentRequest, dcnReference));

        LOG.info("Check for existing DCN in Payment Table Bulk Scan Pay DB");
        EnvelopePayment payment = getPaymentByDcnReference(dcnReference);

        if (null == payment) {
            LOG.info("Create new payment in BSP DB as envelope doesn't exists");
            List<PaymentDto> payments = new ArrayList<>();
            payments.add(paymentDtoMapper.fromRequest(exelaPaymentRequest, dcnReference));

            Envelope envelope = createEnvelope(EnvelopeDto.envelopeDtoWith()
                                                   .paymentStatus(INCOMPLETE)
                                                   .payments(payments)
                                                   .build());
            LOG.info("Update payment status as incomplete");
            return updateEnvelopePaymentStatus(envelope);
        } else {
            if (Optional.ofNullable(payment).isPresent()
                    && Optional.ofNullable(payment.getEnvelope()).isPresent()
                    && payment.getEnvelope().getPaymentStatus().equalsIgnoreCase(INCOMPLETE.toString())) {
                LOG.info("Update payment status as Complete");
                payment.setPaymentStatus(COMPLETE.toString());
                updatePayment(payment);
                return updateEnvelopePaymentStatus(payment.getEnvelope());
            }
        }
        return null;
    }

    @Override
    @Transactional
    public SearchResponse retrieveByCCDReference(String ccdReference) {
        List<EnvelopeCase> envelopeCases = getEnvelopeCaseByCCDReference(SearchRequest.searchRequestWith()
                                                                             .ccdReference(ccdReference)
                                                                             .exceptionRecord(ccdReference)
                                                                             .build());
        List<PaymentMetadata> paymentMetadataList = Optional.ofNullable(envelopeCases).isPresent()
                                                        ? getPaymentMetadataForEnvelopeCase(envelopeCases)
                                                        : null;
        if (Optional.ofNullable(paymentMetadataList).isPresent() && !paymentMetadataList.isEmpty()) {
            return SearchResponse.searchResponseWith()
                .ccdReference(envelopeCases.get(0).getCcdReference())
                .exceptionRecordReference(envelopeCases.get(0).getExceptionRecordReference())
                .responsibleServiceId(envelopeCases.get(0).getEnvelope().getResponsibleServiceId())
                .payments(paymentMetadataDtoMapper.fromPaymentMetadataEntities(paymentMetadataList))
                .build();
        }
        return null;
    }

    @Override
    @Transactional
    public SearchResponse retrieveByDcn(String documentControlNumber) {
        List<EnvelopeCase> envelopeCases = getEnvelopeCaseByDCN(SearchRequest.searchRequestWith()
                                                                    .documentControlNumber(
                                                                        documentControlNumber)
                                                                    .build());
        List<PaymentMetadata> paymentMetadataList = Optional.ofNullable(envelopeCases).isPresent()
            ? getPaymentMetadataForEnvelopeCase(envelopeCases)
            : null;
        if (Optional.ofNullable(paymentMetadataList).isPresent() && !paymentMetadataList.isEmpty()) {
            return SearchResponse.searchResponseWith()
                .ccdReference(envelopeCases.get(0).getCcdReference())
                .exceptionRecordReference(envelopeCases.get(0).getExceptionRecordReference())
                .responsibleServiceId(envelopeCases.get(0).getEnvelope().getResponsibleServiceId())
                .payments(paymentMetadataDtoMapper.fromPaymentMetadataEntities(paymentMetadataList))
                .build();
        }
        return null;
    }

    @Override
    @Transactional
    public Envelope saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest) {
        Envelope envelopeNew = bsPaymentRequestMapper.mapEnvelopeFromBulkScanPaymentRequest(bsPaymentRequest);

        Envelope envelopeDB = bulkScanningUtils.returnExistingEnvelope(envelopeNew);

        //if we have envelope already in BS
        if (Optional.ofNullable(envelopeDB).isPresent() && Optional.ofNullable(envelopeDB.getId()).isPresent()) {
            bulkScanningUtils.handlePaymentStatus(envelopeDB, envelopeNew);
        }

        bulkScanningUtils.insertStatusHistoryAudit(envelopeDB);
        envelopeRepository.save(envelopeDB);
        if(envelopeRepository.findById(envelopeDB.getId()).isPresent()){
            return envelopeRepository.findById(envelopeDB.getId()).get();
        }
        return null;
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
    public String markPaymentAsProcessed(String dcn) {
        Envelope envelopeToMarkProcessed = bulkScanningUtils.markPaymentAsProcessed(dcn);

        if (Optional.ofNullable(envelopeToMarkProcessed).isPresent()) {
            envelopeRepository.save(envelopeToMarkProcessed);
            return dcn;
        }

        return null;
    }

    @Override
    @Transactional
    public PaymentMetadata getPaymentMetadata(String dcnReference) {
        return paymentMetadataRepository.findByDcnReference(dcnReference).orElse(null);
    }

    private List<PaymentMetadata> getPaymentMetadataForEnvelopeCase(List<EnvelopeCase> envelopeCases) {
        List<PaymentMetadata> paymentMetadataList = new ArrayList<>();
        if (Optional.ofNullable(envelopeCases).isPresent() && !envelopeCases.isEmpty()) {
            LOG.info("No of Envelopes exists : {}", envelopeCases.size());
            envelopeCases.stream().forEach(envelopeCase -> {
                envelopeCase.getEnvelope().getEnvelopePayments().stream()
                    .filter(envelopePayment -> envelopePayment.getPaymentStatus().equalsIgnoreCase(COMPLETE.toString()))
                    .forEach(envelopePayment -> {
                        paymentMetadataList.add(getPaymentMetadata(envelopePayment.getDcnReference()));
                    });
            });
        }
        return paymentMetadataList;
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

    private Envelope updateEnvelopePaymentStatus(Envelope envelope) {
        List<EnvelopePayment> payments = paymentRepository.findByEnvelopeId(envelope.getId()).orElse(null);
        if(Optional.ofNullable(payments).isPresent()
                && ! payments.isEmpty()){
            Boolean isPaymentsInComplete = payments.stream().map(payment -> payment.getPaymentStatus())
                .collect(Collectors.toList())
                .contains(INCOMPLETE.toString());
            if (isPaymentsInComplete) {
                updateEnvelopeStatus(envelope, INCOMPLETE);
            } else {
                updateEnvelopeStatus(envelope, COMPLETE);
            }
        }
        return envelope;
    }

    private void updateEnvelopeStatus(Envelope envelope, PaymentStatus paymentStatus) {
        envelope.setPaymentStatus(paymentStatus.toString());
        bulkScanningUtils.insertStatusHistoryAudit(envelope);
        envelopeRepository.save(envelope);
    }

    private Envelope createEnvelope(EnvelopeDto envelopeDto) {
        return envelopeRepository.save(envelopeDtoMapper.toEnvelopeEntity(envelopeDto));
    }

    private List<EnvelopeCase> getEnvelopeCaseByCCDReference(SearchRequest searchRequest) {
        return StringUtils.isNotEmpty(searchRequest.getCcdReference())
            ? envelopeCaseRepository.findByCcdReference(searchRequest.getCcdReference())
            .orElse(envelopeCaseRepository.findByExceptionRecordReference(searchRequest.getExceptionRecord()).orElse(
                null))
            : Collections.emptyList();
    }

    private List<EnvelopeCase> getEnvelopeCaseByDCN(SearchRequest searchRequest) {
        Optional<EnvelopePayment> payment = paymentRepository.findByDcnReference(searchRequest.getDocumentControlNumber());
        EnvelopeCase envelopeCase = payment.isPresent()
            ? envelopeCaseRepository.findByEnvelopeId(payment.get().getEnvelope().getId()).orElse(null)
            : null;
        if(Optional.ofNullable(envelopeCase).isPresent() && StringUtils.isNotEmpty(envelopeCase.getCcdReference())){
            if(envelopeCaseRepository.findByCcdReference(envelopeCase.getCcdReference()).isPresent()){
                return envelopeCaseRepository.findByCcdReference(envelopeCase.getCcdReference()).get();
            }
        }
        return Collections.emptyList();
    }
}
