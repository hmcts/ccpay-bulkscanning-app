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

        return switch (reportType) {
            case UNPROCESSED -> buildUnprocessedReportData(fromDate, toDate);
            case DATA_LOSS -> buildDataLossReportData(fromDate, toDate);
        };
    }

    @Override
    @Transactional
    public List<BaseReportData> retrieveDataByReportType(Date fromDate, Date toDate, ReportType reportType) {
        LOG.info("Retrieving data for Report Type : {}", reportType);

        return switch (reportType) {
            case UNPROCESSED -> buildReportDataUnprocessed(fromDate, toDate);
            case DATA_LOSS -> buildReportDataDataLoss(fromDate, toDate);
        };
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
                    ((ReportDataUnprocessed) record).setCcdRef(StringUtils.defaultString(caseData.getCcdReference()));
                    ((ReportDataUnprocessed) record).setExceptionRef(StringUtils.defaultString(caseData.getExceptionRecordReference()));
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

    private void populateCommonFields(EnvelopePayment payment, Object record, Map<String, PaymentMetadata> metadataByDcn) {
        setPaymentAssetDcn(payment, record);
        var metadata = metadataByDcn.get(payment.getDcnReference());
        if (metadata != null) {
            setPaymentMetadataFields(metadata, record);
        }
        setEnvelopeFields(payment, record);
    }

    private ReportDataDataLoss populateReportDataDataLoss(EnvelopePayment payment, Map<String, PaymentMetadata> metadataByDcn) {
        ReportDataDataLoss record = ReportDataDataLoss.recordWith().build();
        populateCommonFields(payment, record, metadataByDcn);
        String lossResp = Bulk_Scan.toString().equalsIgnoreCase(payment.getSource()) ? FEE_PAY : Bulk_Scan.toString();
        record.setLossResp(lossResp);
        return record;
    }

    private ReportDataUnprocessed populateReportDataUnprocessed(EnvelopePayment payment, Map<String, PaymentMetadata> metadataByDcn) {
        ReportDataUnprocessed record = ReportDataUnprocessed.recordWith().build();
        populateCommonFields(payment, record, metadataByDcn);
        return record;
    }

    private ReportData populateDataLossReportData(EnvelopePayment payment, Map<String, PaymentMetadata> metadataByDcn) {
        ReportData record = ReportData.recordWith().build();
        populateCommonFields(payment, record, metadataByDcn);
        String lossResp = Bulk_Scan.toString().equalsIgnoreCase(payment.getSource()) ? FEE_PAY : Bulk_Scan.toString();
        record.setLossResp(lossResp);
        return record;
    }

    private ReportData populateUnprocessedReportData(EnvelopePayment payment, Map<String, PaymentMetadata> metadataByDcn) {
        ReportData record = ReportData.recordWith().build();
        populateCommonFields(payment, record, metadataByDcn);
        if (payment.getEnvelope() != null && payment.getEnvelope().getEnvelopeCases() != null
            && !payment.getEnvelope().getEnvelopeCases().isEmpty()) {
            var caseData = payment.getEnvelope().getEnvelopeCases().getFirst();
            record.setCcdRef(caseData.getCcdReference());
            record.setExceptionRef(caseData.getExceptionRecordReference());
        }
        return record;
    }

    private Map<String, PaymentMetadata> loadMetadataByDcn(List<EnvelopePayment> payments) {
        if (payments == null || payments.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> dcnRefs = payments.stream()
            .map(EnvelopePayment::getDcnReference)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (dcnRefs.isEmpty()) {
            return Collections.emptyMap();
        }

        return paymentMetadataRepository.findAllByDcnReferenceIn(dcnRefs)
            .stream()
            .filter(Objects::nonNull)
            .filter(m -> m.getDcnReference() != null)
            .collect(Collectors.toMap(PaymentMetadata::getDcnReference, m -> m, (a, b) -> a));
    }

    private List<BaseReportData> buildReportDataDataLoss(Date fromDate, Date toDate) {
        LocalDateTime from = DateUtil.dateToLocalDateTime(fromDate);
        LocalDateTime to = DateUtil.dateToLocalDateTime(toDate);

        List<EnvelopePayment> payments = paymentRepository
            .findForReportByPaymentStatusAndDateCreatedBetween(INCOMPLETE.toString(), from, to)
            .orElse(Collections.emptyList());

        Map<String, PaymentMetadata> metadataByDcn = loadMetadataByDcn(payments);

        return payments.stream()
            .map(p -> populateReportDataDataLoss(p, metadataByDcn))
            .sorted(Comparator.comparing(ReportDataDataLoss::getLossResp))
            .map(r -> (BaseReportData) r)
            .collect(Collectors.toList());
    }

    private List<BaseReportData> buildReportDataUnprocessed(Date fromDate, Date toDate) {
        LocalDateTime from = DateUtil.dateToLocalDateTime(fromDate);
        LocalDateTime to = DateUtil.dateToLocalDateTime(toDate);

        List<EnvelopePayment> payments = paymentRepository
            .findForReportByPaymentStatusAndDateCreatedBetween(COMPLETE.toString(), from, to)
            .orElse(Collections.emptyList());

        Map<String, PaymentMetadata> metadataByDcn = loadMetadataByDcn(payments);

        return payments.stream()
            .map(p -> populateReportDataUnprocessed(p, metadataByDcn))
            .sorted(Comparator.comparing(BaseReportData::getRespServiceId, Comparator.nullsLast(String::compareTo)))
            .map(r -> (BaseReportData) r)
            .collect(Collectors.toList());
    }

    private List<ReportData> buildDataLossReportData(Date fromDate, Date toDate) {
        LocalDateTime from = DateUtil.dateToLocalDateTime(fromDate);
        LocalDateTime to = DateUtil.dateToLocalDateTime(toDate);

        List<EnvelopePayment> payments = paymentRepository
            .findForReportByPaymentStatusAndDateCreatedBetween(INCOMPLETE.toString(), from, to)
            .orElse(Collections.emptyList());

        LOG.info("No of Payments found for Report Type : DATA_LOSS : {}", payments.size());
        Map<String, PaymentMetadata> metadataByDcn = loadMetadataByDcn(payments);

        return payments.stream()
            .map(p -> populateDataLossReportData(p, metadataByDcn))
            .sorted(Comparator.comparing(ReportData::getLossResp))
            .collect(Collectors.toList());
    }

    private List<ReportData> buildUnprocessedReportData(Date fromDate, Date toDate) {
        LocalDateTime from = DateUtil.dateToLocalDateTime(fromDate);
        LocalDateTime to = DateUtil.dateToLocalDateTime(toDate);

        List<EnvelopePayment> payments = paymentRepository
            .findForReportByPaymentStatusAndDateCreatedBetween(COMPLETE.toString(), from, to)
            .orElse(Collections.emptyList());

        LOG.info("No of Payments found for Report Type : UNPROCESSED : {}", payments.size());
        Map<String, PaymentMetadata> metadataByDcn = loadMetadataByDcn(payments);

        return payments.stream()
            .map(p -> populateUnprocessedReportData(p, metadataByDcn))
            .sorted(Comparator.comparing(ReportData::getRespServiceId, Comparator.nullsLast(String::compareTo)))
            .collect(Collectors.toList());
    }
}
