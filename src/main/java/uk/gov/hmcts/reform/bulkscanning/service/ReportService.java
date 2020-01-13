package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;

import java.util.Date;
import java.util.List;

public interface ReportService {
    List<ReportData> retrieveByReportType(Date fromDate, Date toDate, ReportType reportType);

    List<?> retrieveDataByReportType(Date fromDate, Date toDate, ReportType reportType);
}
