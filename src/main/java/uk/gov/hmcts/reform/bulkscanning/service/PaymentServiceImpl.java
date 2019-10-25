package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.audit.AppInsightsAuditRepository;
import uk.gov.hmcts.reform.bulkscanning.exception.ExceptionRecordNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.mapper.BulkScanPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.*;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.SearchRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;
import uk.gov.hmcts.reform.bulkscanning.utils.DateUtil;

import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.EnvelopeSource.*;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.*;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    private final PaymentMetadataRepository paymentMetadataRepository;

    private final EnvelopeRepository envelopeRepository;

    private final EnvelopeCaseRepository envelopeCaseRepository;

    private final AppInsightsAuditRepository auditRepository;

    private final PaymentMetadataDtoMapper paymentMetadataDtoMapper;

    private final EnvelopeDtoMapper envelopeDtoMapper;

    private final PaymentDtoMapper paymentDtoMapper;

    private final BulkScanPaymentRequestMapper bsPaymentRequestMapper;

    private final BulkScanningUtils bulkScanningUtils;

    private static final Logger LOG = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentMetadataRepository paymentMetadataRepository,
                              EnvelopeRepository envelopeRepository,
                              PaymentMetadataDtoMapper paymentMetadataDtoMapper,
                              EnvelopeDtoMapper envelopeDtoMapper,
                              PaymentDtoMapper paymentDtoMapper,
                              BulkScanPaymentRequestMapper bsPaymentRequestMapper,
                              BulkScanningUtils bulkScanningUtils,
                              EnvelopeCaseRepository envelopeCaseRepository,
                              AppInsightsAuditRepository auditRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
        this.envelopeRepository = envelopeRepository;
        this.paymentMetadataDtoMapper = paymentMetadataDtoMapper;
        this.envelopeDtoMapper = envelopeDtoMapper;
        this.paymentDtoMapper = paymentDtoMapper;
        this.bsPaymentRequestMapper = bsPaymentRequestMapper;
        this.bulkScanningUtils = bulkScanningUtils;
        this.envelopeCaseRepository = envelopeCaseRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional
    public Envelope processPaymentFromExela(BulkScanPayment bulkScanPayment, String dcnReference) {
        LOG.info("Insert Payment metadata in Bulk Scan Payment DB");//
        createPaymentMetadata(paymentMetadataDtoMapper.fromRequest(bulkScanPayment, dcnReference));

        LOG.info("Check for existing DCN in Payment Table Bulk Scan Pay DB");
        EnvelopePayment payment = getPaymentByDcnReference(dcnReference);

        if (null == payment) {
            LOG.info("Create new payment in BSP DB as envelope doesn't exists");
            List<PaymentDto> payments = new ArrayList<>();
            payments.add(paymentDtoMapper.fromRequest(bulkScanPayment, dcnReference));

            Envelope envelope = createEnvelope(EnvelopeDto.envelopeDtoWith()
                                                   .paymentStatus(INCOMPLETE)
                                                   .payments(payments)
                                                   .build());
            LOG.info("Envelope created with status as incomplete");
            auditRepository.trackPaymentEvent("EXELA_PAYMENT", envelope.getEnvelopePayments().get(0));
            return envelope;
        } else {
            if (Optional.ofNullable(payment.getEnvelope()).isPresent()
                && payment.getEnvelope().getPaymentStatus().equalsIgnoreCase(INCOMPLETE.toString())) {
                LOG.info("Update payment status as Complete");
                payment.setPaymentStatus(COMPLETE.toString());
                LOG.info("Updating Payment Source to BOTH as we have received payment from Bulk_Scan & Excela");
                payment.setSource(Both.toString());
                updatePayment(payment);
                auditRepository.trackPaymentEvent("EXELA_PAYMENT", payment);
                return updateEnvelopePaymentStatus(payment.getEnvelope(), COMPLETE);
            }
        }
        return null;
    }

    @Override
    @Transactional
    public SearchResponse retrieveByCCDReference(String ccdReference) {

        List<EnvelopeCase> envelopeCases = getEnvelopeCaseByCCDReference(SearchRequest.searchRequestWith()
                                                                             .ccdReference(ccdReference)
                                                                             .exceptionRecord(ccdReference)
                                                                             .build());
        return searchForEnvelopeCasePayments(envelopeCases);
    }

    @Override
    @Transactional
    public SearchResponse retrieveByDcn(String documentControlNumber) {
        List<EnvelopeCase> envelopeCases = getEnvelopeCaseByDCN(SearchRequest.searchRequestWith()
                                                                    .documentControlNumber(
                                                                        documentControlNumber)
                                                                    .build());
        if(envelopeCases == null){
            // No Payment exists for the searched DCN
            LOG.info("Payment Not exists for the searched DCN !!!");
            return null;
        }
        if(envelopeCases.isEmpty()){
            return SearchResponse.searchResponseWith()
                .allPaymentsStatus(INCOMPLETE)
                .build();
        }
        return searchForEnvelopeCasePayments(envelopeCases);
    }

    @Override
    @Transactional
    public List<String> saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest) {
        List<String> listOfAllPayments = new ArrayList<>();

        Envelope envelopeNew = bsPaymentRequestMapper.mapEnvelopeFromBulkScanPaymentRequest(bsPaymentRequest);
        List<Envelope> listOfExistingEnvelope = bulkScanningUtils.returnExistingEnvelopeList(envelopeNew);

        if (Optional.ofNullable(listOfExistingEnvelope).isPresent() && !listOfExistingEnvelope.isEmpty()) {
            for (Envelope envelopeDB: listOfExistingEnvelope) {
                //if we have envelope already in BS
                if (Optional.ofNullable(envelopeDB).isPresent() && Optional.ofNullable(envelopeDB.getId()).isPresent()) {
                    LOG.info("Existing envelope found for Bulk Scan request");
                    bulkScanningUtils.handlePaymentStatus(envelopeDB, envelopeNew);
                }

                bulkScanningUtils.insertStatusHistoryAudit(envelopeDB);
                envelopeRepository.save(envelopeDB);

                if(Optional.ofNullable(envelopeDB.getEnvelopePayments()).isPresent()
                    && ! envelopeDB.getEnvelopePayments().isEmpty()){
                    envelopeDB.getEnvelopePayments().stream().forEach(payment -> {
                        auditRepository.trackPaymentEvent("Bulk-Scan_PAYMENT", payment);
                    });
                }

                Optional<Envelope> envelope = envelopeRepository.findById(envelopeDB.getId());

                List<String> paymentDCNList = envelope.get().getEnvelopePayments().stream().map(envelopePayment -> envelopePayment.getDcnReference()).collect(
                    Collectors.toList());

                listOfAllPayments.addAll(paymentDCNList);
            }
        }

        if (Optional.ofNullable(listOfAllPayments).isPresent() && !listOfAllPayments.isEmpty()) {
            return listOfAllPayments;
        }
        return null;
    }

    @Override
    @Transactional
    public String updateCaseReferenceForExceptionRecord(String exceptionRecordReference, CaseReferenceRequest caseReferenceRequest) {
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

            envelopeCaseRepository.saveAll(envelopeCases);
            return envelopeCases.stream().map(envelopeCase -> envelopeCase.getId().toString()).collect(Collectors.toList())
                .stream().collect(Collectors.joining(","));
        }

        return "";
    }

    @Override
    @Transactional
    public String updatePaymentStatus(String dcn, PaymentStatus status) {
        Envelope envelope = bulkScanningUtils.updatePaymentStatus(dcn, status);
        if (Optional.ofNullable(envelope).isPresent()) {
            envelopeRepository.save(envelope);
            updateEnvelopePaymentStatus(envelope, PROCESSED);
            if(Optional.ofNullable(envelope.getEnvelopePayments()).isPresent()
                && ! envelope.getEnvelopePayments().isEmpty()){
                envelope.getEnvelopePayments().stream().forEach(payment -> {
                    auditRepository.trackPaymentEvent("PAYMENT_STATUS_UPDATE", payment);
                });
            }
            return dcn;
        }
        return null;
    }

    @Override
    @Transactional
    public PaymentMetadata getPaymentMetadata(String dcnReference) {
        return paymentMetadataRepository.findByDcnReference(dcnReference).orElse(null);
    }

    @Override
    @Transactional
    public List<ReportData> retrieveByReportType(Date fromDate, Date toDate, ReportType reportType) {
        List<ReportData> reportDataList = new ArrayList<>();
        if (reportType.equals(ReportType.UNPROCESSED)) {
            Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatus(COMPLETE.toString());
            if (payments.isPresent()) {
                payments.get().stream()
                    .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                    && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                    .forEach(payment -> {
                    ReportData record = ReportData.recordWith().build();
                    record.setPaymentAssetDcn(payment.getDcnReference());
                    Optional<PaymentMetadata> paymentMetadata = paymentMetadataRepository.findByDcnReference(payment.getDcnReference());
                    if (paymentMetadata.isPresent()) {
                        record.setDateBanked(paymentMetadata.get().getDateBanked().toString());
                        record.setBgcBatch(paymentMetadata.get().getBgcReference());
                        record.setPaymentMethod(paymentMetadata.get().getPaymentMethod());
                        record.setAmount(paymentMetadata.get().getAmount());
                    }
                    if (Optional.ofNullable(payment.getEnvelope()).isPresent()) {
                        record.setRespServiceId(payment.getEnvelope().getResponsibleServiceId());
                        if(Optional.ofNullable(payment.getEnvelope().getResponsibleServiceId()).isPresent()
                            && Optional.ofNullable(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId())).isPresent()){
                            record.setRespServiceName(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId()).value());
                        }
                        if (Optional.ofNullable(payment.getEnvelope().getEnvelopeCases()).isPresent()
                            && !payment.getEnvelope().getEnvelopeCases().isEmpty()) {
                            record.setCcdRef(payment.getEnvelope().getEnvelopeCases().get(0).getCcdReference());
                            record.setExceptionRef(payment.getEnvelope().getEnvelopeCases().get(0).getExceptionRecordReference());
                        }
                    }
                    reportDataList.add(record);
                });
                reportDataList.sort(Comparator.comparing(ReportData::getRespServiceId));
            }
            return reportDataList;
        }
        if (reportType.equals(ReportType.DATA_LOSS)) {
            Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatus(INCOMPLETE.toString());
            if (payments.isPresent()) {
                payments.get().stream()
                    .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                        && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                    .forEach(payment -> {
                    ReportData record = ReportData.recordWith().build();
                    record.setPaymentAssetDcn(payment.getDcnReference());
                    Optional<PaymentMetadata> paymentMetadata = paymentMetadataRepository.findByDcnReference(payment.getDcnReference());
                    if (paymentMetadata.isPresent()) {
                        record.setDateBanked(paymentMetadata.get().getDateBanked().toString());
                        record.setBgcBatch(paymentMetadata.get().getBgcReference());
                        record.setPaymentMethod(paymentMetadata.get().getPaymentMethod());
                        record.setAmount(paymentMetadata.get().getAmount());
                    }
                    if (Optional.ofNullable(payment.getEnvelope()).isPresent()) {
                        record.setRespServiceId(payment.getEnvelope().getResponsibleServiceId());
                        if(Optional.ofNullable(payment.getEnvelope().getResponsibleServiceId()).isPresent()
                                && Optional.ofNullable(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId())).isPresent()){
                            record.setRespServiceName(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId()).value());
                        }
                    }
                    String lossResp = payment.getSource().equalsIgnoreCase(Bulk_Scan.toString())
                                        ? Exela.toString() : Bulk_Scan.toString();
                    record.setLossResp(lossResp);
                    reportDataList.add(record);
                });
                reportDataList.sort(Comparator.comparing(ReportData::getLossResp));
            }
            return reportDataList;
        }
        return null;
    }

    @Override
    @Transactional
    public List<?> retrieveDataByReportType(Date fromDate, Date toDate, ReportType reportType) {

        if (reportType.equals(ReportType.UNPROCESSED)) {
            List<ReportDataUnprocessed> reportDataList = new ArrayList<>();
            Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatus(COMPLETE.toString());
            if (payments.isPresent()) {
                payments.get().stream()
                    .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                        && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                    .forEach(payment -> {
                        ReportDataUnprocessed record = ReportDataUnprocessed.recordWith().build();
                        record.setPaymentAssetDcn(payment.getDcnReference());
                        Optional<PaymentMetadata> paymentMetadata = paymentMetadataRepository.findByDcnReference(payment.getDcnReference());
                        if (paymentMetadata.isPresent()) {
                            record.setDateBanked(paymentMetadata.get().getDateBanked().toString());
                            record.setBgcBatch(paymentMetadata.get().getBgcReference());
                            record.setPaymentMethod(paymentMetadata.get().getPaymentMethod());
                            record.setAmount(paymentMetadata.get().getAmount());
                        }
                        if (Optional.ofNullable(payment.getEnvelope()).isPresent()) {
                            record.setRespServiceId(payment.getEnvelope().getResponsibleServiceId());
                            if(Optional.ofNullable(payment.getEnvelope().getResponsibleServiceId()).isPresent()
                                && Optional.ofNullable(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId())).isPresent()){
                                record.setRespServiceName(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId()).value());
                            }
                            if (Optional.ofNullable(payment.getEnvelope().getEnvelopeCases()).isPresent()
                                && !payment.getEnvelope().getEnvelopeCases().isEmpty()) {
                                String ccdRef = Optional.ofNullable(payment.getEnvelope().getEnvelopeCases().get(0).getCcdReference()).isPresent()
                                                    ? payment.getEnvelope().getEnvelopeCases().get(0).getCcdReference()
                                                    : StringUtils.EMPTY;
                                record.setCcdRef(ccdRef);
                                String exceptionRef = Optional.ofNullable(payment.getEnvelope().getEnvelopeCases().get(0).getExceptionRecordReference()).isPresent()
                                    ? payment.getEnvelope().getEnvelopeCases().get(0).getExceptionRecordReference()
                                    : StringUtils.EMPTY;
                                record.setExceptionRef(exceptionRef);
                            }
                        }
                        reportDataList.add(record);
                    });
                reportDataList.sort(Comparator.comparing(ReportDataUnprocessed::getRespServiceId));
            }
            return reportDataList;
        }
        if (reportType.equals(ReportType.DATA_LOSS)) {
            List<ReportDataDataLoss> reportDataList = new ArrayList<>();
            Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatus(INCOMPLETE.toString());
            if (payments.isPresent()) {
                payments.get().stream()
                    .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                        && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                    .forEach(payment -> {
                    ReportDataDataLoss record = ReportDataDataLoss.recordWith().build();
                    record.setPaymentAssetDcn(payment.getDcnReference());
                    Optional<PaymentMetadata> paymentMetadata = paymentMetadataRepository.findByDcnReference(payment.getDcnReference());
                    if (paymentMetadata.isPresent()) {
                        record.setDateBanked(paymentMetadata.get().getDateBanked().toString());
                        record.setBgcBatch(paymentMetadata.get().getBgcReference());
                        record.setPaymentMethod(paymentMetadata.get().getPaymentMethod());
                        record.setAmount(paymentMetadata.get().getAmount());
                    }
                    if (Optional.ofNullable(payment.getEnvelope()).isPresent()) {
                        record.setRespServiceId(payment.getEnvelope().getResponsibleServiceId());
                        if(Optional.ofNullable(payment.getEnvelope().getResponsibleServiceId()).isPresent()
                            && Optional.ofNullable(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId())).isPresent()){
                            record.setRespServiceName(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId()).value());
                        }
                    }
                    String lossResp = payment.getSource().equalsIgnoreCase(Bulk_Scan.toString())
                        ? Exela.toString() : Bulk_Scan.toString();
                    record.setLossResp(lossResp);
                    reportDataList.add(record);
                });
                reportDataList.sort(Comparator.comparing(ReportDataDataLoss::getLossResp));
            }
            return reportDataList;
        }
        return null;
    }

    private SearchResponse searchForEnvelopeCasePayments(List<EnvelopeCase> envelopeCases) {
        SearchResponse searchResponse = SearchResponse.searchResponseWith().build();
        if(Optional.ofNullable(envelopeCases).isPresent() && !envelopeCases.isEmpty()
            && Optional.ofNullable(envelopeCases.get(0).getEnvelope()).isPresent()){
            if(checkForAllEnvelopesStatus(envelopeCases, PROCESSED.toString())){
                searchResponse.setAllPaymentsStatus(PROCESSED);
            }else if(checkForAllEnvelopesStatus(envelopeCases, COMPLETE.toString())){
                searchResponse.setAllPaymentsStatus(COMPLETE);
            }else{
                searchResponse.setAllPaymentsStatus(INCOMPLETE);
            }
            if(searchResponse.getAllPaymentsStatus().equals(INCOMPLETE)
                    || searchResponse.getAllPaymentsStatus().equals(COMPLETE)){
                List<PaymentMetadata> paymentMetadataList = getPaymentMetadataForEnvelopeCase(envelopeCases);
                if (Optional.ofNullable(paymentMetadataList).isPresent()
                    && !paymentMetadataList.isEmpty()) {
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
            .stream().allMatch(status :: equals);
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

    private EnvelopePayment getPaymentByDcnReference(String dcnReference) {
        return paymentRepository.findByDcnReference(dcnReference).orElse(null);
    }

    private EnvelopePayment updatePayment(EnvelopePayment payment) {
        return paymentRepository.save(payment);
    }

    private PaymentMetadata createPaymentMetadata(PaymentMetadataDto paymentMetadataDto) {
        PaymentMetadata paymentMetadata = paymentMetadataDtoMapper.toPaymentEntity(paymentMetadataDto);
        return paymentMetadataRepository.save(paymentMetadata);
    }

    private Envelope updateEnvelopePaymentStatus(Envelope envelope, PaymentStatus paymentStatus) {
        List<EnvelopePayment> payments = paymentRepository.findByEnvelopeId(envelope.getId()).orElse(null);
        if (checkAllPaymentsStatus(paymentStatus, payments)) {
            updateEnvelopeStatus(envelope, paymentStatus);
        }else if (checkAnyPaymentsStatus(INCOMPLETE, payments)){
            updateEnvelopeStatus(envelope, INCOMPLETE);
        }else if (checkAnyPaymentsStatus(COMPLETE, payments)){
            updateEnvelopeStatus(envelope, COMPLETE);
        }
        return envelope;
    }

    private boolean checkAllPaymentsStatus(PaymentStatus paymentStatus, List<EnvelopePayment> payments) {
        return Optional.ofNullable(payments).isPresent()
            && !payments.isEmpty()
            && payments.stream()
                            .map(payment -> payment.getPaymentStatus())
                            .collect(Collectors.toList())
                            .stream().allMatch(paymentStatus.toString() :: equals);
    }

    private boolean checkAnyPaymentsStatus(PaymentStatus paymentStatus, List<EnvelopePayment> payments) {
        return Optional.ofNullable(payments).isPresent()
            && ! payments.isEmpty()
            && ! payments.stream()
            .filter(payment -> payment.getPaymentStatus().equalsIgnoreCase(paymentStatus.toString()))
            .collect(Collectors.toList())
            .isEmpty();
    }

    private Envelope updateEnvelopeStatus(Envelope envelope, PaymentStatus paymentStatus) {
        envelope.setPaymentStatus(paymentStatus.toString());
        bulkScanningUtils.insertStatusHistoryAudit(envelope);
        return envelopeRepository.save(envelope);
    }

    private Envelope createEnvelope(EnvelopeDto envelopeDto) {
        Envelope envelope = envelopeRepository.save(envelopeDtoMapper.toEnvelopeEntity(envelopeDto));
        bulkScanningUtils.insertStatusHistoryAudit(envelope);
        return envelope;
    }

    private List<EnvelopeCase> getEnvelopeCaseByCCDReference(SearchRequest searchRequest) {
        if (StringUtils.isNotEmpty(searchRequest.getCcdReference())
            && envelopeCaseRepository.findByCcdReference(searchRequest.getCcdReference()).isPresent()) {
            return envelopeCaseRepository.findByCcdReference(searchRequest.getCcdReference()).get();
        } else if (StringUtils.isNotEmpty(searchRequest.getExceptionRecord())
            && envelopeCaseRepository.findByExceptionRecordReference(searchRequest.getExceptionRecord()).isPresent()) {
            List<EnvelopeCase> envelopeCases = envelopeCaseRepository.findByExceptionRecordReference(searchRequest.getExceptionRecord()).get();
            for (EnvelopeCase envelopeCase : envelopeCases) {
                if (StringUtils.isNotEmpty(envelopeCase.getCcdReference())
                    && envelopeCaseRepository.findByCcdReference(envelopeCase.getCcdReference()).isPresent()) {
                    return envelopeCaseRepository.findByCcdReference(envelopeCase.getCcdReference()).get();
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
                return this.getEnvelopeCaseByCCDReference(searchRequest);
            }
            return Collections.emptyList();
        }
        return null;
    }
}
