package uk.gov.hmcts.reform.bulkscanning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.mapper.BulkScanPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

@Service
public class BulkScanConsumerServiceImpl implements BulkScanConsumerService {

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    BulkScanningUtils bulkScanningUtils;

    @Autowired
    BulkScanPaymentRequestMapper bsPaymentRequestMapper;

    @Override
    public void saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest) {
        Envelope envelope = bsPaymentRequestMapper.mapEnvelopeFromBulkScanPaymentRequest(bsPaymentRequest);

        Envelope existingEnvelope = bulkScanningUtils.returnExistingEnvelope(envelope);
        bulkScanningUtils.handlePaymentStatus(existingEnvelope);

        bulkScanningUtils.insertStatusHistoryAudit(existingEnvelope);
        envelopeRepository.save(existingEnvelope);
    }
}
