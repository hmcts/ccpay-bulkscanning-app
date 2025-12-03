package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.model.dto.BaseReportData;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.EnvelopeSource.Bulk_Scan;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;

@Service
public class ReportServiceImpl implements ReportService {

    private final PaymentRepository paymentRepository;

    private final PaymentMetadataRepository paymentMetadataRepository;

    private static final Logger LOG = LoggerFactory.getLogger(ReportServiceImpl.class);

    private static final String FEE_PAY = "Fee_Pay";

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
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public List<BaseReportData> retrieveDataByReportType(Date fromDate, Date toDate, ReportType reportType) {
        LOG.info("Retrieving data for Report Type : {}", reportType);
        if (reportType.equals(ReportType.UNPROCESSED)) {
            return buildReportDataUnprocessed(fromDate, toDate);
        }
        if (reportType.equals(ReportType.DATA_LOSS)) {
            return buildReportDataDataLoss(fromDate, toDate);
        }
        return Collections.emptyList();
    }

    private void setPaymentMetadataFields(PaymentMetadata metadata, Object record) {
        if (record instanceof ReportData) {
            ((ReportData) record).setDateBanked(metadata.getDateBanked().toString());
            ((ReportData) record).setBgcBatch(metadata.getBgcReference());
            ((ReportData) record).setPaymentMethod(metadata.getPaymentMethod());
            ((ReportData) record).setAmount(metadata.getAmount());
        } else if (record instanceof ReportDataDataLoss) {
            ((ReportDataDataLoss) record).setDateBanked(metadata.getDateBanked().toString());
            ((ReportDataDataLoss) record).setBgcBatch(metadata.getBgcReference());
            ((ReportDataDataLoss) record).setPaymentMethod(metadata.getPaymentMethod());
            ((ReportDataDataLoss) record).setAmount(metadata.getAmount());
        } else if (record instanceof ReportDataUnprocessed) {
            ((ReportDataUnprocessed) record).setDateBanked(metadata.getDateBanked().toString());
            ((ReportDataUnprocessed) record).setBgcBatch(metadata.getBgcReference());
            ((ReportDataUnprocessed) record).setPaymentMethod(metadata.getPaymentMethod());
            ((ReportDataUnprocessed) record).setAmount(metadata.getAmount());
        }
    }

    private void setEnvelopeFields(EnvelopePayment payment, Object record) {
        if (payment.getEnvelope() != null) {
            String respServiceId = payment.getEnvelope().getResponsibleServiceId();
            if (record instanceof ReportData) {
                ((ReportData) record).setRespServiceId(respServiceId);
                if (respServiceId != null) {
                    ResponsibleSiteId siteId = ResponsibleSiteId.valueOf(respServiceId);
                    ((ReportData) record).setRespServiceName(siteId.value());
                }
            } else if (record instanceof ReportDataDataLoss) {
                ((ReportDataDataLoss) record).setRespServiceId(respServiceId);
                if (respServiceId != null) {
                    ResponsibleSiteId siteId = ResponsibleSiteId.valueOf(respServiceId);
                    ((ReportDataDataLoss) record).setRespServiceName(siteId.value());
                }
            } else if (record instanceof ReportDataUnprocessed) {
                ((ReportDataUnprocessed) record).setRespServiceId(respServiceId);
                if (respServiceId != null) {
                    ResponsibleSiteId siteId = ResponsibleSiteId.valueOf(respServiceId);
                    ((ReportDataUnprocessed) record).setRespServiceName(siteId.value());
                }
                if (payment.getEnvelope().getEnvelopeCases() != null && !payment.getEnvelope().getEnvelopeCases().isEmpty()) {
                    var caseData = payment.getEnvelope().getEnvelopeCases().stream().findFirst().orElse(null);
                    if (caseData != null) {
                        ((ReportDataUnprocessed) record).setCcdRef(StringUtils.defaultString(caseData.getCcdReference()));
                        ((ReportDataUnprocessed) record).setExceptionRef(StringUtils.defaultString(caseData.getExceptionRecordReference()));
                    }
                }
            }
        }
    }

    private void setPaymentAssetDcn(EnvelopePayment payment, Object record) {
        if (record instanceof ReportData) {
            ((ReportData) record).setPaymentAssetDcn(payment.getDcnReference());
        } else if (record instanceof ReportDataDataLoss) {
            ((ReportDataDataLoss) record).setPaymentAssetDcn(payment.getDcnReference());
        } else if (record instanceof ReportDataUnprocessed) {
            ((ReportDataUnprocessed) record).setPaymentAssetDcn(payment.getDcnReference());
        }
    }

    private void populateCommonFields(EnvelopePayment payment, Object record) {
        setPaymentAssetDcn(payment, record);
        paymentMetadataRepository.findByDcnReference(payment.getDcnReference())
            .ifPresent(metadata -> setPaymentMetadataFields(metadata, record));
        setEnvelopeFields(payment, record);
    }

    private ReportDataDataLoss populateReportDataDataLoss(EnvelopePayment payment) {
        ReportDataDataLoss record = ReportDataDataLoss.recordWith().build();
        populateCommonFields(payment, record);
        String lossResp = Bulk_Scan.toString().equalsIgnoreCase(payment.getSource()) ? FEE_PAY : Bulk_Scan.toString();
        record.setLossResp(lossResp);
        return record;
    }

    private ReportDataUnprocessed populateReportDataUnprocessed(EnvelopePayment payment) {
        ReportDataUnprocessed record = ReportDataUnprocessed.recordWith().build();
        populateCommonFields(payment, record);
        return record;
    }

    private ReportData populateDataLossReportData(EnvelopePayment payment) {
        ReportData record = ReportData.recordWith().build();
        populateCommonFields(payment, record);
        String lossResp = Bulk_Scan.toString().equalsIgnoreCase(payment.getSource()) ? FEE_PAY : Bulk_Scan.toString();
        record.setLossResp(lossResp);
        return record;
    }

    private ReportData populateUnprocessedReportData(EnvelopePayment payment) {
        ReportData record = ReportData.recordWith().build();
        populateCommonFields(payment, record);
        if (payment.getEnvelope() != null && payment.getEnvelope().getEnvelopeCases() != null
            && !payment.getEnvelope().getEnvelopeCases().isEmpty()) {
            var caseData = payment.getEnvelope().getEnvelopeCases().stream().findFirst().orElse(null);
            if (caseData != null) {
                record.setCcdRef(caseData.getCcdReference());
                record.setExceptionRef(caseData.getExceptionRecordReference());
            }
        }
        return record;
    }

    private List<BaseReportData> buildReportDataDataLoss(Date fromDate, Date toDate) {
        LocalDateTime from = DateUtil.dateToLocalDateTime(fromDate);
        LocalDateTime to = DateUtil.dateToLocalDateTime(toDate);
        return paymentRepository.findByPaymentStatusAndDateCreatedBetween(INCOMPLETE.toString(), from, to)
            .map(payments -> payments.stream()
                .map(this::populateReportDataDataLoss)
                .sorted(Comparator.comparing(ReportDataDataLoss::getLossResp))
                .map(r -> (BaseReportData) r)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    private List<BaseReportData> buildReportDataUnprocessed(Date fromDate, Date toDate) {
        LocalDateTime from = DateUtil.dateToLocalDateTime(fromDate);
        LocalDateTime to = DateUtil.dateToLocalDateTime(toDate);
        return paymentRepository.findByPaymentStatusAndDateCreatedBetween(COMPLETE.toString(), from, to)
            .map(payments -> payments.stream()
                .map(this::populateReportDataUnprocessed)
                .sorted(Comparator.comparing(BaseReportData::getRespServiceId))
                .map(r -> (BaseReportData) r)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    private List<ReportData> buildDataLossReportData(Date fromDate, Date toDate, List<ReportData> reportDataList) {
        LocalDateTime from = DateUtil.dateToLocalDateTime(fromDate);
        LocalDateTime to = DateUtil.dateToLocalDateTime(toDate);
        Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatusAndDateCreatedBetween(INCOMPLETE.toString(), from, to);
        if (payments.isPresent()) {
            LOG.info("No of Payments found for Report Type : DATA_LOSS : {}", payments.get().size());
            payments.get().stream()
                .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                    && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                .map(this::populateDataLossReportData)
                .sorted(Comparator.comparing(ReportData::getLossResp))
                .forEach(reportDataList::add);
        }
        return reportDataList;
    }

    private List<ReportData> buildUnprocessedReportData(Date fromDate, Date toDate, List<ReportData> reportDataList) {
        LocalDateTime from = DateUtil.dateToLocalDateTime(fromDate);
        LocalDateTime to = DateUtil.dateToLocalDateTime(toDate);
        Optional<List<EnvelopePayment>> payments = paymentRepository.findByPaymentStatusAndDateCreatedBetween(COMPLETE.toString(), from, to);
        if (payments.isPresent()) {
            LOG.info("No of Payments found for Report Type : UNPROCESSED : {}", payments.get().size());
            payments.get().stream()
                .filter(payment -> DateUtil.localDateTimeToDate(payment.getDateCreated()).after(fromDate)
                    && DateUtil.localDateTimeToDate(payment.getDateCreated()).before(toDate))
                .map(this::populateUnprocessedReportData)
                .sorted(Comparator.comparing(ReportData::getRespServiceId))
                .forEach(reportDataList::add);
        }
        return reportDataList;
    }
}
