package uk.gov.hmcts.reform.bulkscanning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentMetadataDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.StatusHistoryDTO;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.StatusHistoryDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;
import uk.gov.hmcts.reform.bulkscanning.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.repository.StatusHistoryRepository;

import java.time.LocalDateTime;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMetadataRepository paymentMetadataRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final EnvelopeRepository envelopeRepository;

    private final PaymentDTOMapper paymentDTOMapper;
    private final PaymentMetadataDTOMapper paymentMetadataDTOMapper;
    private final StatusHistoryDTOMapper statusHistoryDTOMapper;
    private final EnvelopeDTOMapper envelopeDTOMapper;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentMetadataRepository paymentMetadataRepository,
                              StatusHistoryRepository statusHistoryRepository,
                              EnvelopeRepository envelopeRepository,
                              PaymentDTOMapper paymentDTOMapper,
                              PaymentMetadataDTOMapper paymentMetadataDTOMapper,
                              StatusHistoryDTOMapper statusHistoryDTOMapper,
                              EnvelopeDTOMapper envelopeDTOMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.envelopeRepository = envelopeRepository;
        this.paymentDTOMapper = paymentDTOMapper;
        this.paymentMetadataDTOMapper = paymentMetadataDTOMapper;
        this.statusHistoryDTOMapper = statusHistoryDTOMapper;
        this.envelopeDTOMapper = envelopeDTOMapper;
    }


    @Override
    public Payment getPaymentByDcnReference(String dcnReference) {
        return paymentRepository.findByDcnReference(dcnReference).orElse(null);
    }

    @Override
    public Payment createPayment(PaymentDTO paymentDto) {
        return paymentRepository.save(paymentDTOMapper.toPaymentEntity(paymentDto));
    }

    @Override
    public Payment updatePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public PaymentMetadata createPaymentMetadata(PaymentMetadataDTO paymentMetadataDto) {
        return paymentMetadataRepository.save(paymentMetadataDTOMapper.toPaymentEntity(paymentMetadataDto));
    }

    @Override
    public StatusHistory createStatusHistory(StatusHistoryDTO statusHistoryDto) {
        return statusHistoryRepository.save(statusHistoryDTOMapper.toStatusHistoryEntity(statusHistoryDto));
    }

    @Override
    public Envelope createEnvelope(EnvelopeDTO envelopeDto) {
        Envelope envelop = envelopeDTOMapper.toEnvelopeEntity(envelopeDto);
        envelop.setDateCreated(LocalDateTime.now());
        envelop.getPayments().stream().forEach(payment -> payment.setDateCreated(LocalDateTime.now()));
        return envelopeRepository.save(envelop);
    }
}
