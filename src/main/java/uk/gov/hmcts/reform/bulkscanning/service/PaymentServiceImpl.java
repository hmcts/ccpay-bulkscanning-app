package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.StatusHistoryDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.StatusHistoryDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.*;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.repository.*;
import uk.gov.hmcts.reform.bulkscanning.model.request.SearchRequest;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.localDateTimeToDate;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMetadataRepository paymentMetadataRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeCaseRepository envelopeCaseRepository;

    private final PaymentMetadataDtoMapper paymentMetadataDtoMapper;
    private final StatusHistoryDtoMapper statusHistoryDtoMapper;
    private final EnvelopeDtoMapper envelopeDtoMapper;
    private final BulkScanningUtils bulkScanningUtils;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentMetadataRepository paymentMetadataRepository,
                              StatusHistoryRepository statusHistoryRepository,
                              EnvelopeRepository envelopeRepository,
                              PaymentMetadataDtoMapper paymentMetadataDtoMapper,
                              StatusHistoryDtoMapper statusHistoryDtoMapper,
                              EnvelopeDtoMapper envelopeDtoMapper,
                              BulkScanningUtils bulkScanningUtils,
                              EnvelopeCaseRepository envelopeCaseRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.envelopeRepository = envelopeRepository;
        this.paymentMetadataDtoMapper = paymentMetadataDtoMapper;
        this.statusHistoryDtoMapper = statusHistoryDtoMapper;
        this.envelopeDtoMapper = envelopeDtoMapper;
        this.bulkScanningUtils = bulkScanningUtils;
        this.envelopeCaseRepository = envelopeCaseRepository;
    }


    @Override
    public EnvelopePayment getPaymentByDcnReference(String dcnReference) {
        return paymentRepository.findByDcnReference(dcnReference).orElse(null);
    }

    @Override
    public EnvelopePayment updatePayment(EnvelopePayment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public PaymentMetadata createPaymentMetadata(PaymentMetadataDto paymentMetadataDto) {
        PaymentMetadata paymentMetadata = paymentMetadataDtoMapper.toPaymentEntity(paymentMetadataDto);

        return paymentMetadataRepository.save(paymentMetadata);
    }

    @Override
    public PaymentMetadata getPaymentMetadata(String dcnReference) {
        return paymentMetadataRepository.findByDcnReference(dcnReference).orElse(null);
    }

    @Override
    public StatusHistory createStatusHistory(StatusHistoryDto statusHistoryDto) {
        try {
            statusHistoryDto.setDateCreated(localDateTimeToDate(LocalDateTime.now()));
            StatusHistory statusHistory = statusHistoryDtoMapper.toStatusHistoryEntity(statusHistoryDto);
            return statusHistoryRepository.save(statusHistory);
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @Override
    public Envelope updateEnvelopePaymentStatus(Envelope envelope) {
        List<EnvelopePayment> payments = paymentRepository.findByEnvelopeId(envelope.getId()).orElse(null);
        Boolean isPaymentsInComplete = payments.stream().map(payment -> payment.getPaymentStatus())
            .collect(Collectors.toList())
            .contains(INCOMPLETE.toString());
        if (isPaymentsInComplete) {
            updateEnvelopeStatus(envelope, INCOMPLETE);
        } else {
            updateEnvelopeStatus(envelope, PaymentStatus.COMPLETE);
        }
        return envelope;
    }

    private void updateEnvelopeStatus(Envelope envelope, PaymentStatus paymentStatus) {
        envelope.setPaymentStatus(paymentStatus.toString());
        envelopeRepository.save(envelope);
        bulkScanningUtils.insertStatusHistoryAudit(envelope);
    }

    @Override
    public Envelope createEnvelope(EnvelopeDto envelopeDto) {
        return envelopeRepository.save(envelopeDtoMapper.toEnvelopeEntity(envelopeDto));
    }

    @Override
    public EnvelopeCase getEnvelopeCaseByCCDReference(SearchRequest searchRequest) {
        return StringUtils.isNotEmpty(searchRequest.getCcdReference())
                    ? envelopeCaseRepository.findByCcdReference(searchRequest.getCcdReference()).orElse(null)
                    : envelopeCaseRepository.findByExceptionRecordReference(searchRequest.getExceptionRecord()).orElse(null);
    }

    @Override
    public EnvelopeCase getEnvelopeCaseByDCN(SearchRequest searchRequest) {
        Optional<EnvelopePayment> payment = paymentRepository.findByDcnReference(searchRequest.getDocumentControlNumber());
        return payment.isPresent()
            ? envelopeCaseRepository.findByEnvelopeId(payment.get()
                                                          .getEnvelope()
                                                          .getId())
                                                            .orElse(EnvelopeCase.caseWith()
                                                                        .envelope(Envelope.envelopeWith()
                                                                                      .envelopePayments(getPayments(payment.get()))
                                                                                      .build())
                                                                        .build())
            : null;
    }

    public List<EnvelopePayment> getPayments(EnvelopePayment payment){
        List<EnvelopePayment> paymentList = new ArrayList<>();
        paymentList.add(payment);
        return paymentList;
    }
}
