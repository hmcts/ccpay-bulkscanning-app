package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportDataDataLoss;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportDataUnprocessed;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.utils.DateUtil;

import java.util.*;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.EnvelopeSource.Bulk_Scan;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.EnvelopeSource.Exela;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;

@Service
public class ReportServiceImpl implements ReportService {

    private final PaymentRepository paymentRepository;

    private final PaymentMetadataRepository paymentMetadataRepository;

    private static final Logger LOG = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    public ReportServiceImpl(PaymentRepository paymentRepository,
                             PaymentMetadataRepository paymentMetadataRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMetadataRepository = paymentMetadataRepository;
    }

    @Override
    @Transactional
    public List<ReportData> retrieveByReportType(Date fromDate, Date toDate, ReportType reportType) {
        LOG.info("Retrieving data for Report Type : {}", reportType);
        List<ReportData> reportDataList = new ArrayList<>();
        if (reportType.equals(ReportType.UNPROCESSED)) {
            return buildUnprocessedReportData(fromDate, toDate, reportDataList);
        }
        if (reportType.equals(ReportType.DATA_LOSS)) {
            return buildDataLossReportData(fromDate, toDate, reportDataList);
        }
        return null;
    }

    @Override
    @Transactional
    public List<?> retrieveDataByReportType(Date fromDate, Date toDate, ReportType reportType) {
        LOG.info("Retrieving data for Report Type : {}", reportType);
        if (reportType.equals(ReportType.UNPROCESSED)) {
            return buildReportDataUnprocessed(fromDate, toDate);
        }
        if (reportType.equals(ReportType.DATA_LOSS)) {
            return buildReportDataDataLoss(fromDate, toDate);
        }
        return null;
    }

    private List<?> buildReportDataDataLoss(Date fromDate, Date toDate) {
        List<ReportDataDataLoss> reportDataList = new ArrayList<>();
        Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatus(INCOMPLETE.toString());
        if (payments.isPresent()) {
            LOG.info("No of Payments found for Report Type : DATA_LOSS : {}", payments.get().size());
            payments.get().stream()
                .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                    && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                .forEach(payment -> {
                    ReportDataDataLoss record = populateReportDataDataLoss(payment);
                    reportDataList.add(record);
                });
            reportDataList.sort(Comparator.comparing(ReportDataDataLoss::getLossResp));
        }
        return reportDataList;
    }

    private List<?> buildReportDataUnprocessed(Date fromDate, Date toDate) {
        List<ReportDataUnprocessed> reportDataList = new ArrayList<>();
        Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatus(COMPLETE.toString());
        if (payments.isPresent()) {
            LOG.info("No of Payments found for Report Type : UNPROCESSED : {}", payments.get().size());
            payments.get().stream()
                .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                    && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                .forEach(payment -> {
                    reportDataList.add(populateReportDataUnprocessed(payment));
                });
            reportDataList.sort(Comparator.comparing(ReportDataUnprocessed::getRespServiceId));
        }
        return reportDataList;
    }

    private ReportDataDataLoss populateReportDataDataLoss(EnvelopePayment payment) {
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
            if (Optional.ofNullable(payment.getEnvelope().getResponsibleServiceId()).isPresent()
                && Optional.ofNullable(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId())).isPresent()) {
                record.setRespServiceName(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId()).value());
            }
        }
        String lossResp = payment.getSource().equalsIgnoreCase(Bulk_Scan.toString())
            ? Exela.toString() : Bulk_Scan.toString();
        record.setLossResp(lossResp);
        return record;
    }

    private ReportDataUnprocessed populateReportDataUnprocessed(EnvelopePayment payment) {
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
            if (Optional.ofNullable(payment.getEnvelope().getResponsibleServiceId()).isPresent()
                && Optional.ofNullable(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId())).isPresent()) {
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
        return record;
    }

    private List<ReportData> buildDataLossReportData(Date fromDate, Date toDate, List<ReportData> reportDataList) {
        Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatus(INCOMPLETE.toString());
        if (payments.isPresent()) {
            LOG.info("No of Payments found for Report Type : DATA_LOSS : {}", payments.get().size());
            payments.get().stream()
                .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                    && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                .forEach(payment -> {
                    reportDataList.add(populateDataLossReportData(payment));
                });
            reportDataList.sort(Comparator.comparing(ReportData::getLossResp));
        }
        return reportDataList;
    }

    private List<ReportData> buildUnprocessedReportData(Date fromDate, Date toDate, List<ReportData> reportDataList) {
        Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatus(COMPLETE.toString());
        if (payments.isPresent()) {
            LOG.info("No of Payments found for Report Type : UNPROCESSED : {}", payments.get().size());
            payments.get().stream()
                .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                    && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                .forEach(payment -> {
                    reportDataList.add(populateUnprocessedReportData(payment));
                });
            reportDataList.sort(Comparator.comparing(ReportData::getRespServiceId));
        }
        return reportDataList;
    }

    private ReportData populateDataLossReportData(EnvelopePayment payment) {
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
            if (Optional.ofNullable(payment.getEnvelope().getResponsibleServiceId()).isPresent()
                && Optional.ofNullable(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId())).isPresent()) {
                record.setRespServiceName(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId()).value());
            }
        }
        String lossResp = payment.getSource().equalsIgnoreCase(Bulk_Scan.toString())
            ? Exela.toString() : Bulk_Scan.toString();
        record.setLossResp(lossResp);
        return record;
    }

    private ReportData populateUnprocessedReportData(EnvelopePayment payment) {
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
            if (Optional.ofNullable(payment.getEnvelope().getResponsibleServiceId()).isPresent()
                && Optional.ofNullable(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId())).isPresent()) {
                record.setRespServiceName(ResponsibleSiteId.valueOf(payment.getEnvelope().getResponsibleServiceId()).value());
            }
            if (Optional.ofNullable(payment.getEnvelope().getEnvelopeCases()).isPresent()
                && !payment.getEnvelope().getEnvelopeCases().isEmpty()) {
                record.setCcdRef(payment.getEnvelope().getEnvelopeCases().get(0).getCcdReference());
                record.setExceptionRef(payment.getEnvelope().getEnvelopeCases().get(0).getExceptionRecordReference());
            }
        }
        return record;
    }
}
