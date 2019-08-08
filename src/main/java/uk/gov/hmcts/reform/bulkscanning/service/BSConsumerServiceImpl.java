package uk.gov.hmcts.reform.bulkscanning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.mapper.BSPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.BSPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
@Service
public class BSConsumerServiceImpl implements BSConsumerService{

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    BSPaymentRequestMapper bsPaymentRequestMapper;

    @Override
    public void saveInitialMetadataFromBS(BSPaymentRequest bsPaymentRequest) {
        Payment payment = bsPaymentRequestMapper.mapPaymentFromBSPaymentRequest(bsPaymentRequest);
        paymentRepository.save(payment);
    }
}
