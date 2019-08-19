package uk.gov.hmcts.reform.bulkscanning.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanning.mapper.BulkScanPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

import java.util.Optional;

@Service
public class BulkScanConsumerServiceImpl implements BulkScanConsumerService{

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    BulkScanningUtils bulkScanningUtils;

    @Autowired
    BulkScanPaymentRequestMapper bsPaymentRequestMapper;

    @Override
    public String saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest) {
        Envelope envelopeNew = bsPaymentRequestMapper.mapEnvelopeFromBulkScanPaymentRequest(bsPaymentRequest);

        Envelope envelopeDB = bulkScanningUtils.returnExistingEnvelope(envelopeNew);

        //if we have envelope already in BS
        if (Optional.ofNullable(envelopeDB).isPresent() && Optional.ofNullable(envelopeDB.getId()).isPresent()) {
                bulkScanningUtils.handlePaymentStatus(envelopeDB, envelopeNew);
        }

        bulkScanningUtils.insertStatusHistoryAudit(envelopeDB);
        return envelopeRepository.save(envelopeDB).getId().toString();
    }
}
