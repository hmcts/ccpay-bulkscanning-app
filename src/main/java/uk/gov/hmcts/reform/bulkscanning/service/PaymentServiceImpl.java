package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang3.StringUtils;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;


@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMetadataRepository paymentMetadataRepository;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private EnvelopeCaseRepository envelopeCaseRepository;

    @Autowired
    private PaymentMetadataDtoMapper paymentMetadataDtoMapper;

    @Autowired
    private EnvelopeDtoMapper envelopeDtoMapper;

    @Autowired
    private PaymentDtoMapper paymentDtoMapper;

    @Autowired
    private BulkScanPaymentRequestMapper bsPaymentRequestMapper;

    @Autowired
    private BulkScanningUtils bulkScanningUtils;

    @Override
    public void processPaymentFromExela(ExelaPaymentRequest exelaPaymentRequest, String dcnReference) {
        //Insert Payment metadata in BSP DB
        createPaymentMetadata(paymentMetadataDtoMapper.fromRequest(exelaPaymentRequest, dcnReference));

        //Check for existing DCN in Payment Table Bulk Scan Pay DB,
        EnvelopePayment payment = getPaymentByDcnReference(dcnReference);

        if (null == payment) {
            //Create new payment in BSP DB if envelope doesn't exists
            List<PaymentDto> payments = new ArrayList<>();
            payments.add(paymentDtoMapper.fromRequest(exelaPaymentRequest, dcnReference));

            Envelope envelope = createEnvelope(EnvelopeDto.envelopeDtoWith()
                                                   .paymentStatus(INCOMPLETE)
                                                   .payments(payments)
                                                   .build());
            //Update payment status as incomplete
            updateEnvelopePaymentStatus(envelope);
        } else {
            if (payment.getEnvelope().getPaymentStatus().equalsIgnoreCase(INCOMPLETE.toString())) {
                //07-08-2019 Update payment status as complete
                payment.setPaymentStatus(COMPLETE.toString());
                payment.setDateUpdated(LocalDateTime.now());
                updatePayment(payment);
                updateEnvelopePaymentStatus(payment.getEnvelope());
            }
        }
    }

    @Override
    @Transactional
    public SearchResponse retrieveByCCDReference(String ccdReference) {
        List<EnvelopeCase> envelopeCases = getEnvelopeCaseByCCDReference(SearchRequest.searchRequestWith()
                                                                             .ccdReference(ccdReference)
                                                                             .exceptionRecord(ccdReference)
                                                                             .build());
        List<PaymentMetadata> paymentMetadataList = getPaymentMetadataForEnvelopeCase(envelopeCases);
        if (!paymentMetadataList.isEmpty()) {
            return SearchResponse.searchResponseWith()
                .ccdReference(envelopeCases.get(0).getCcdReference())
                .exceptionRecordReference(envelopeCases.get(0).getExceptionRecordReference())
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
        List<PaymentMetadata> paymentMetadataList = getPaymentMetadataForEnvelopeCase(envelopeCases);
        if (!paymentMetadataList.isEmpty()) {
            return SearchResponse.searchResponseWith()
                .ccdReference(envelopeCases.get(0).getCcdReference())
                .exceptionRecordReference(envelopeCases.get(0).getExceptionRecordReference())
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
        return envelopeRepository.findById(envelopeDB.getId()).get();
    }

    @Override
    @Transactional
    public String updateCaseReferenceForExceptionRecord(String exceptionRecordReference, CaseReferenceRequest caseReferenceRequest) {
        //TODO yet to handle multiple envelopes with same exception reference scenario
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
        }
        return envelopeCaseRepository.save(envelopeCases.get(0)).getId().toString();
    }

    @Override
    @Transactional
    public String markPaymentAsProcessed(String dcn) {
        return envelopeRepository.save(bulkScanningUtils.markPaymentAsProcessed(dcn)).getId().toString();
    }

    @Override
    @Transactional
    public PaymentMetadata getPaymentMetadata(String dcnReference) {
        return paymentMetadataRepository.findByDcnReference(dcnReference).orElse(null);
    }

    private List<PaymentMetadata> getPaymentMetadataForEnvelopeCase(List<EnvelopeCase> envelopeCases) {
        List<PaymentMetadata> paymentMetadataList = new ArrayList<>();
        if (Optional.ofNullable(envelopeCases).isPresent() && !envelopeCases.isEmpty()) {
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

    @Transactional
    private EnvelopePayment getPaymentByDcnReference(String dcnReference) {
        return paymentRepository.findByDcnReference(dcnReference).orElse(null);
    }

    @Transactional
    private EnvelopePayment updatePayment(EnvelopePayment payment) {
        return paymentRepository.save(payment);
    }

    @Transactional
    private PaymentMetadata createPaymentMetadata(PaymentMetadataDto paymentMetadataDto) {
        PaymentMetadata paymentMetadata = paymentMetadataDtoMapper.toPaymentEntity(paymentMetadataDto);

        return paymentMetadataRepository.save(paymentMetadata);
    }



    @Transactional
    private Envelope updateEnvelopePaymentStatus(Envelope envelope) {
        List<EnvelopePayment> payments = paymentRepository.findByEnvelopeId(envelope.getId()).orElse(null);
        Boolean isPaymentsInComplete = payments.stream().map(payment -> payment.getPaymentStatus())
            .collect(Collectors.toList())
            .contains(INCOMPLETE.toString());
        if (isPaymentsInComplete) {
            updateEnvelopeStatus(envelope, INCOMPLETE);
        } else {
            updateEnvelopeStatus(envelope, COMPLETE);
        }
        return envelope;
    }

    @Transactional
    private void updateEnvelopeStatus(Envelope envelope, PaymentStatus paymentStatus) {
        envelope.setPaymentStatus(paymentStatus.toString());
        envelopeRepository.save(envelope);
        bulkScanningUtils.insertStatusHistoryAudit(envelope);
    }

    @Transactional
    private Envelope createEnvelope(EnvelopeDto envelopeDto) {
        return envelopeRepository.save(envelopeDtoMapper.toEnvelopeEntity(envelopeDto));
    }

    @Transactional
    private List<EnvelopeCase> getEnvelopeCaseByCCDReference(SearchRequest searchRequest) {
        return StringUtils.isNotEmpty(searchRequest.getCcdReference())
            ? envelopeCaseRepository.findByCcdReference(searchRequest.getCcdReference())
            .orElse(envelopeCaseRepository.findByExceptionRecordReference(searchRequest.getExceptionRecord()).orElse(
                null))
            : null;
    }

    @Transactional
    private List<EnvelopeCase> getEnvelopeCaseByDCN(SearchRequest searchRequest) {
        Optional<EnvelopePayment> payment = paymentRepository.findByDcnReference(searchRequest.getDocumentControlNumber());
        EnvelopeCase envelopeCase = payment.isPresent()
            ? envelopeCaseRepository.findByEnvelopeId(payment.get().getEnvelope().getId()).orElse(null)
            : null;
        return StringUtils.isNotEmpty(envelopeCase.getCcdReference())
            ? envelopeCaseRepository.findByCcdReference(envelopeCase.getCcdReference()).orElse(null)
            : null;
    }
}
