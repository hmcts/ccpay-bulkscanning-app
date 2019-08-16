package uk.gov.hmcts.reform.bulkscanning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.StatusHistoryDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.StatusHistoryDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.StatusHistoryRepository;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.localDateTimeToDate;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMetadataRepository paymentMetadataRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final EnvelopeRepository envelopeRepository;

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
                              BulkScanningUtils bulkScanningUtils) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.envelopeRepository = envelopeRepository;
        this.paymentMetadataDtoMapper = paymentMetadataDtoMapper;
        this.statusHistoryDtoMapper = statusHistoryDtoMapper;
        this.envelopeDtoMapper = envelopeDtoMapper;
        this.bulkScanningUtils = bulkScanningUtils;
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
        paymentMetadataDto.setDateCreated(localDateTimeToDate(LocalDateTime.now()));
        PaymentMetadata paymentMetadata = paymentMetadataDtoMapper.toPaymentEntity(paymentMetadataDto);

        return paymentMetadataRepository.save(paymentMetadata);
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
}
