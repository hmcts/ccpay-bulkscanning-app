package uk.gov.hmcts.reform.bulkscanning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentMetadataDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.StatusHistoryDTO;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.StatusHistoryDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.repository.StatusHistoryRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMetadataRepository paymentMetadataRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final EnvelopeRepository envelopeRepository;

    private final PaymentMetadataDTOMapper paymentMetadataDTOMapper;
    private final StatusHistoryDTOMapper statusHistoryDTOMapper;
    private final EnvelopeDTOMapper envelopeDTOMapper;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentMetadataRepository paymentMetadataRepository,
                              StatusHistoryRepository statusHistoryRepository,
                              EnvelopeRepository envelopeRepository,
                              PaymentMetadataDTOMapper paymentMetadataDTOMapper,
                              StatusHistoryDTOMapper statusHistoryDTOMapper,
                              EnvelopeDTOMapper envelopeDTOMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.envelopeRepository = envelopeRepository;
        this.paymentMetadataDTOMapper = paymentMetadataDTOMapper;
        this.statusHistoryDTOMapper = statusHistoryDTOMapper;
        this.envelopeDTOMapper = envelopeDTOMapper;
    }


    @Override
    public Payment getPaymentByDcnReference(String dcnReference) {
        return paymentRepository.findByDcnReference(dcnReference).orElse(null);
    }

    @Override
    public Payment updatePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public PaymentMetadata createPaymentMetadata(PaymentMetadataDTO paymentMetadataDto) {
        paymentMetadataDto.setDateCreated(localDateTimeToDate(LocalDateTime.now()));
        PaymentMetadata paymentMetadata = paymentMetadataDTOMapper.toPaymentEntity(paymentMetadataDto);

        return paymentMetadataRepository.save(paymentMetadata);
    }

    @Override
    public StatusHistory createStatusHistory(StatusHistoryDTO statusHistoryDto) {
        StatusHistory statusHistory = statusHistoryDTOMapper.toStatusHistoryEntity(statusHistoryDto);
        statusHistory.setDateCreated(LocalDateTime.now());
        return statusHistoryRepository.save(statusHistory);
    }

    @Override
    public Envelope updateEnvelopePaymentStatus(Envelope envelope) {
        List<Payment> payments = paymentRepository.findByEnvelopeId(envelope.getId()).orElse(null);
        Boolean isPaymentsInComplete = payments.stream().map(payment -> payment.getPaymentStatus())
                                                        .collect(Collectors.toList())
                                                        .contains(PaymentStatus.INCOMPLETE);
        if(isPaymentsInComplete){
            updateEnvelopeStatus(envelope, PaymentStatus.INCOMPLETE);
        }else{
            updateEnvelopeStatus(envelope, PaymentStatus.COMPLETE);
        }
        return envelope;
    }

    private void updateEnvelopeStatus(Envelope envelope, PaymentStatus paymentStatus) {
        envelope.setPaymentStatus(paymentStatus);
        envelope.setDateUpdated(LocalDateTime.now());
        envelopeRepository.save(envelope);
        /*createStatusHistory(StatusHistoryDTO.envelopeDtoWith()
            .envelope(envelopeDTOMapper.fromEnvelopeEntity(envelope))
            .status(paymentStatus)
            .build());*/
    }

    @Override
    public Envelope createEnvelope(EnvelopeDTO envelopeDto) {
        Envelope envelop = envelopeDTOMapper.toEnvelopeEntity(envelopeDto);
        envelop.setDateCreated(LocalDateTime.now());
        envelop.getPayments().stream().forEach(payment -> payment.setDateCreated(LocalDateTime.now()));
        return envelopeRepository.save(envelop);
    }

    public Date localDateTimeToDate(LocalDateTime ldt){
        return ldt != null ? Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    public LocalDateTime dateToLocalDateTime(Date date){
        return date != null ? LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()) : null;
    }
}
