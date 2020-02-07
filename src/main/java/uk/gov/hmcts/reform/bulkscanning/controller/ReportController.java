package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.dto.BaseReportData;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;
import uk.gov.hmcts.reform.bulkscanning.service.ReportService;
import uk.gov.hmcts.reform.bulkscanning.utils.ExcelGeneratorUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.*;

@RestController
@Api(tags = {"Bulk Scanning Payment Report API"})
@SwaggerDefinition(tags = {@Tag(name = "ReportController",
    description = "Bulk Scanning Payment Report API to be used for generating Audit report")})
public class ReportController {

    private static final Logger LOG = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @ApiOperation("API to generate Report for Bulk_Scan_Payment System")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Report Generated"),
        @ApiResponse(code = 404, message = "No Data found to generate Report")
    })
    @GetMapping("/report/download")
    public ResponseEntity<byte[]> retrieveByReportType(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("date_from") Date fromDate,
        @RequestParam("date_to") Date toDate,
        @RequestParam("report_type") ReportType reportType,
        HttpServletResponse response) {
        LOG.info("Retrieving payments for reportType : {}", reportType);
        byte[] reportBytes = null;
        HSSFWorkbook workbook = null;
        ByteArrayOutputStream baos = null;
        try {
            workbook = new HSSFWorkbook();
            baos = new ByteArrayOutputStream();
            List<ReportData> reportDataList = reportService
                        .retrieveByReportType(atStartOfDay(fromDate), atEndOfDay(toDate), reportType);
            if (Optional.ofNullable(reportDataList).isPresent()) {
                LOG.info("No of Records exists : {}", reportDataList.size());
                workbook = (HSSFWorkbook) ExcelGeneratorUtil.exportToExcel(reportType, reportDataList);
            }
            workbook.write(baos);
            reportBytes = baos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.ms-excel"));
            String fileName = reportType.toString() + "_"
                + getDateForReportName(fromDate) + "_To_"
                + getDateForReportName(toDate) + "_RUN_"
                + getDateTimeForReportName(new Date(System.currentTimeMillis()))
                + ".xls";
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            return new ResponseEntity<byte[]>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception ex) {
            throw new PaymentException(ex);
        } finally {
            try {
                baos.close();
                workbook.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }

        }
    }

    @ApiOperation("API to retrieve Report Data from Bulk_Scan_Payment System")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Report Generated"),
        @ApiResponse(code = 404, message = "No Data found to generate Report")
    })
    @GetMapping("/report/data")
    public ResponseEntity<List<BaseReportData>> retrieveDataByReportType(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("date_from") Date fromDate,
        @RequestParam("date_to") Date toDate,
        @RequestParam("report_type") ReportType reportType) {
        LOG.info("Retrieving payments for reportType : {}", reportType);

        try {
            List<BaseReportData> reportDataList = reportService
                .retrieveDataByReportType(atStartOfDay(fromDate), atEndOfDay(toDate), reportType);
            if (Optional.ofNullable(reportDataList).isPresent()) {
                LOG.info("No of Records exists : {}", reportDataList.size());
                return new ResponseEntity<List<BaseReportData>>(reportDataList, HttpStatus.OK);
            }else {
                LOG.info("No Data found for ReportType : {}", reportType);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }
}
