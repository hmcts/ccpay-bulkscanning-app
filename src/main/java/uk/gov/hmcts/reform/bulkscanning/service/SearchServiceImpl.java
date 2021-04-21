package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.SearchRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.*;


@Service
public class SearchServiceImpl implements SearchService {

    private final PaymentRepository paymentRepository;

    private final PaymentMetadataRepository paymentMetadataRepository;

    private final EnvelopeCaseRepository envelopeCaseRepository;

    private final PaymentMetadataDtoMapper paymentMetadataDtoMapper;

    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    public SearchServiceImpl(PaymentRepository paymentRepository,
                             PaymentMetadataRepository paymentMetadataRepository,
                             PaymentMetadataDtoMapper paymentMetadataDtoMapper,
                             EnvelopeCaseRepository envelopeCaseRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.paymentMetadataDtoMapper = paymentMetadataDtoMapper;
        this.envelopeCaseRepository = envelopeCaseRepository;
    }

    @Override
    @Transactional
    public SearchResponse retrieveByCCDReference(String ccdReference) {

        List<EnvelopeCase> envelopeCases = getEnvelopeCaseByCcdReference(SearchRequest.searchRequestWith()
                                                                             .ccdReference(ccdReference)
                                                                             .exceptionRecord(ccdReference)
                                                                             .build());
        return searchForEnvelopeCasePayments(envelopeCases);
    }

    @Override
    @Transactional
    public SearchResponse retrieveByDcn(String documentControlNumber) {
        if(paymentEnvelopeCheck(SearchRequest.searchRequestWith().documentControlNumber(documentControlNumber).build())) {

            List<EnvelopeCase> envelopeCases = getEnvelopeCaseByDCN(SearchRequest.searchRequestWith()
                                                                        .documentControlNumber(
                                                                            documentControlNumber)
                                                                        .build());

            if (envelopeCases.isEmpty()) {
                return SearchResponse.searchResponseWith()
                    .allPaymentsStatus(INCOMPLETE)
                    .build();
            }
            return searchForEnvelopeCasePayments(envelopeCases);
        } else {
            // No Payment exists for the searched DCN
            LOG.info("Payment Not exists for the searched DCN !!!");
            return null;
        }
    }

    public PaymentMetadata getPaymentMetadata(String dcnReference) {
        return paymentMetadataRepository.findByDcnReference(dcnReference).orElse(null);
    }

    private SearchResponse searchForEnvelopeCasePayments(List<EnvelopeCase> envelopeCases) {
        SearchResponse searchResponse = SearchResponse.searchResponseWith().build();
        if (Optional.ofNullable(envelopeCases).isPresent() && !envelopeCases.isEmpty()
            && Optional.ofNullable(envelopeCases.get(0).getEnvelope()).isPresent()) {
            if (checkForAllEnvelopesStatus(envelopeCases, PROCESSED.toString())) {
                searchResponse.setAllPaymentsStatus(PROCESSED);
            } else if (checkForAllEnvelopesStatus(envelopeCases, COMPLETE.toString())) {
                searchResponse.setAllPaymentsStatus(COMPLETE);
            } else {
                searchResponse.setAllPaymentsStatus(INCOMPLETE);
            }
            if (searchResponse.getAllPaymentsStatus().equals(INCOMPLETE)
                || searchResponse.getAllPaymentsStatus().equals(COMPLETE)) {
                List<PaymentMetadata> paymentMetadataList = getPaymentMetadataForEnvelopeCase(envelopeCases);
                if (!paymentMetadataList.isEmpty()) {
                    searchResponse.setPayments(paymentMetadataDtoMapper.fromPaymentMetadataEntities(paymentMetadataList));
                }
            }
            searchResponse.setCcdReference(envelopeCases.get(0).getCcdReference());
            searchResponse.setExceptionRecordReference(envelopeCases.get(0).getExceptionRecordReference());
            searchResponse.setResponsibleServiceId(envelopeCases.get(0).getEnvelope().getResponsibleServiceId());
        }
        return searchResponse;
    }

    private Boolean checkForAllEnvelopesStatus(List<EnvelopeCase> envelopeCases, String status) {
        return envelopeCases.stream()
            .map(envelopeCase -> envelopeCase.getEnvelope().getPaymentStatus())
            .collect(Collectors.toList())
            .stream().allMatch(status::equals);
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

    private List<EnvelopeCase> getEnvelopeCaseByCcdReference(SearchRequest searchRequest) {
        if (StringUtils.isNotEmpty(searchRequest.getCcdReference())
            && envelopeCaseRepository.findByCcdReference(searchRequest.getCcdReference()).isPresent()) {
            return envelopeCaseRepository.findByCcdReference(searchRequest.getCcdReference()).orElse(Collections.emptyList());
        } else if (StringUtils.isNotEmpty(searchRequest.getExceptionRecord())
            && envelopeCaseRepository.findByExceptionRecordReference(searchRequest.getExceptionRecord()).isPresent()) {
            List<EnvelopeCase> envelopeCases = envelopeCaseRepository.findByExceptionRecordReference(searchRequest.getExceptionRecord()).orElse(Collections.emptyList());
            for (EnvelopeCase envelopeCase : envelopeCases) {
                if (StringUtils.isNotEmpty(envelopeCase.getCcdReference())
                    && envelopeCaseRepository.findByCcdReference(envelopeCase.getCcdReference()).isPresent()) {
                    return envelopeCaseRepository.findByCcdReference(envelopeCase.getCcdReference()).orElse(Collections.emptyList());
                }
            }
            return envelopeCases;
        }
        return Collections.emptyList();
    }

    private List<EnvelopeCase> getEnvelopeCaseByDCN(SearchRequest searchRequest) {
        Optional<EnvelopePayment> payment = paymentRepository.findByDcnReference(searchRequest.getDocumentControlNumber());
        if (payment.isPresent()
            && Optional.ofNullable(payment.get().getEnvelope()).isPresent()) {
            EnvelopeCase envelopeCase = envelopeCaseRepository.findByEnvelopeId(payment.get().getEnvelope().getId()).orElse(
                null);
            if (Optional.ofNullable(envelopeCase).isPresent()) {
                searchRequest.setCcdReference(envelopeCase.getCcdReference());
                searchRequest.setExceptionRecord(envelopeCase.getExceptionRecordReference());
                return this.getEnvelopeCaseByCcdReference(searchRequest);
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    //Check if DCN exists in payment envelope
    private boolean paymentEnvelopeCheck(SearchRequest searchRequest){
        Optional<EnvelopePayment> payment = paymentRepository.findByDcnReference(searchRequest.getDocumentControlNumber());
        return payment.isPresent();
    }
}
