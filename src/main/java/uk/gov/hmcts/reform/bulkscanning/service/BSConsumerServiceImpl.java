package uk.gov.hmcts.reform.bulkscanning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.mapper.BSPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.BSPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

@Service
public class BSConsumerServiceImpl implements BSConsumerService{

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    BulkScanningUtils bulkScanningUtils;

    @Autowired
    BSPaymentRequestMapper bsPaymentRequestMapper;

    @Override
    public void saveInitialMetadataFromBS(BSPaymentRequest bsPaymentRequest) {
        Envelope envelope = bsPaymentRequestMapper.mapEnvelopeFromBSPaymentRequest(bsPaymentRequest);

        bulkScanningUtils.handlePaymentStatus(envelope);
        bulkScanningUtils.insertStatusHistoryAudit(envelope);
        envelopeRepository.save(envelope);
    }


}
