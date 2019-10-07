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
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.PaymentResponse;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;
import uk.gov.hmcts.reform.bulkscanning.utils.ExcelGeneratorUtil;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.*;

@RestController
@Api(tags = {"Bulk Scanning Payment API"})
@SwaggerDefinition(tags = {@Tag(name = "BSPaymentController",
    description = "Bulk Scanning Payment API to be used by the scanning supplier to share the "
        + "payment information contained in the envelope")})
public class PaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @ApiOperation(value = "Get the initial meta data from bulk Scanning",
        notes = "Get the initial meta data from bulk Scanning")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Bulk Scanning Data retrieved"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorization"),
        @ApiResponse(code = 409, message = "Conflict")
    })
    @PostMapping("/bulk-scan-payments")
    public ResponseEntity<PaymentResponse> consumeInitialMetaDataBulkScanning(@Valid @RequestBody BulkScanPaymentRequest bsPaymentRequest) {
        LOG.info("Request received from Bulk Scan Payment : {}", bsPaymentRequest);
        return new ResponseEntity<>(PaymentResponse.paymentResponseWith()
                                        .paymentDcns(paymentService.saveInitialMetadataFromBs(bsPaymentRequest)
                                                         .getEnvelopePayments().stream()
                                                         .map(payment -> payment.getDcnReference())
                                                         .collect(Collectors.toList()))
                                        .build(), HttpStatus.CREATED);
    }

    @ApiOperation("Provide meta information about the payments contained\n" +
        "in the envelope. This operation will be called after the banking process\n" +
        "has been done and payments have been allocated to a BGC slip / batch")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Bulk Scanning Data retrieved"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 409, message = "Conflict")
    })
    @PostMapping("/bulk-scan-payment")
    public ResponseEntity<String> processPaymentFromExela(
        @Valid @RequestBody BulkScanPayment bulkScanPayment) {
        LOG.info("Request received from Exela with Request : {}", bulkScanPayment);
        try {
            LOG.info("Check in Payment metadata for already existing payment from Exela");
            if (Optional.ofNullable(paymentService.getPaymentMetadata(bulkScanPayment.getDcnReference())).isPresent()) {
                LOG.info("Payment already exists for DCN: {}", bulkScanPayment.getDcnReference());
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            } else {
                LOG.info("Processing Payment for DCN: {}", bulkScanPayment.getDcnReference());
                paymentService.processPaymentFromExela(bulkScanPayment, bulkScanPayment.getDcnReference());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body("Created");
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @ApiOperation("API Endpoint to update case reference for payment")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorisation"),
        @ApiResponse(code = 404, message = "Provided exception reference doesn't exist"),
    })
    @PutMapping("/bulk-scan-payments")
    public ResponseEntity updateCaseReferenceForExceptionRecord(
        @NotEmpty @RequestParam("exception_reference") String exceptionRecordReference,
        @Valid @RequestBody CaseReferenceRequest caseReferenceRequest) {

        LOG.info(
            "Request received to update case reference {}, for exception record {}",
            caseReferenceRequest,
            exceptionRecordReference
        );
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(paymentService.updateCaseReferenceForExceptionRecord(
                exceptionRecordReference,
                caseReferenceRequest
            ));
    }

    @ApiOperation("API Endpoint to mark payment as processed")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorisation"),
        @ApiResponse(code = 404, message = "No record exists for provided DCN"),
    })
    @PatchMapping("/bulk-scan-payments/{dcn}/status/{status}")
    public ResponseEntity markPaymentAsProcessed(
        @RequestHeader("Authorization") String authorization,
        @NotEmpty @PathVariable("dcn") String dcn,
        @NotEmpty @PathVariable("status") PaymentStatus status) {
        LOG.info("Request received to mark payment with DCN : {} , status : {}", dcn, status);
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(paymentService.updatePaymentStatus(dcn, status));
    }


    @ApiOperation("Case with unprocessed payments details by CCD Case Reference/Exception Record")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases/{ccd_reference}")
    public ResponseEntity<SearchResponse> retrieveByCCD(
        @RequestHeader("Authorization") String authorization,
        @PathVariable("ccd_reference") String ccdReference) {
        LOG.info("Retrieving payments for ccdReference {} : ", ccdReference);
        try {
            SearchResponse searchResponse = paymentService.retrieveByCCDReference(ccdReference);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                LOG.info("SearchResponse : {}", searchResponse);
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            } else {
                LOG.info("Payments Not found for ccdReference : {}", ccdReference);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @ApiOperation("Case with unprocessed payment details by Payment DCN")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases")
    public ResponseEntity<SearchResponse> retrieveByDCN(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("document_control_number") String documentControlNumber) {
        LOG.info("Retrieving payments for documentControlNumber : {}", documentControlNumber);
        try {
            SearchResponse searchResponse = paymentService.retrieveByDcn(documentControlNumber);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                LOG.info("SearchResponse : {}", searchResponse);
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            } else {
                LOG.info("Payments not found for documentControlNumber : {}", documentControlNumber);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @ApiOperation("API to generate Report for Bulk_Scan_Payment System")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Report Generated"),
        @ApiResponse(code = 404, message = "No Data found to generate Report")
    })
    @GetMapping("/report/download")
    public ResponseEntity<?> retrieveByReportType(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("date_from") Date fromDate,
        @RequestParam("date_to") Date toDate,
        @RequestParam("report_type") ReportType reportType,
        HttpServletResponse response) {
        LOG.info("Retrieving payments for reportType : {}", reportType);
        byte[] reportBytes = null;
        HSSFWorkbook workbook = new HSSFWorkbook();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            List<ReportData> reportDataList = paymentService
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
    public ResponseEntity<List<?>> retrieveDataByReportType(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("date_from") Date fromDate,
        @RequestParam("date_to") Date toDate,
        @RequestParam("report_type") ReportType reportType) {
        LOG.info("Retrieving payments for reportType : {}", reportType);

        try {
            List<?> reportDataList = paymentService
                .retrieveDataByReportType(atStartOfDay(fromDate), atEndOfDay(toDate), reportType);
            if (Optional.ofNullable(reportDataList).isPresent()) {
                LOG.info("No of Records exists : {}", reportDataList.size());
                return new ResponseEntity<>(reportDataList, HttpStatus.OK);
            }else {
                LOG.info("No Data found for ReportType : {}", reportType);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    /*@PostMapping("/bulk-scan-payments-load")
    public ResponseEntity<Integer> loadBsPayments(@RequestBody BulkScanPaymentRequest bulkScanPaymentRequest,
                                                  @RequestParam(value = "count") Integer count) {
        String ccd = bulkScanPaymentRequest.getCcdCaseNumber();
        List<String> dcnList = Arrays.asList(bulkScanPaymentRequest.getDocumentControlNumbers());
        IntStream.range(0, count).forEach(i -> {
            bulkScanPaymentRequest.setCcdCaseNumber( ccd + i);
            List<String> dcns = new ArrayList<>();
            for(String dcn : dcnList){
                dcns.add(dcn + i);
            }
            bulkScanPaymentRequest.setDocumentControlNumbers(dcns.toArray(new String[0]));
            paymentService.saveInitialMetadataFromBs(bulkScanPaymentRequest);
        });
        *//*ExecutorService service = Executors.newFixedThreadPool(1);
        IntStream.range(0, count)
            .forEach(i -> service.submit(() -> {

            }));*//*

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/exela-payments-load")
    public ResponseEntity<Integer> loadExelaPayments(@RequestBody BulkScanPayment exelaPaymentRequest,
                                                     @RequestParam(required = true, value = "DCN") String dcn,
                                                     @RequestParam(required = true, value = "count") Integer count) {
        String bgc = exelaPaymentRequest.getBankGiroCreditSlipNumber();
        IntStream.range(0, count).forEach(i -> {
            String tempDcn = dcn + i;
            exelaPaymentRequest.setBankGiroCreditSlipNumber(bgc + i);
            if (! Optional.ofNullable(paymentService.getPaymentMetadata(tempDcn)).isPresent()) {
                paymentService.processPaymentFromExela(exelaPaymentRequest, tempDcn);
            }
        });
        *//*ExecutorService service = Executors.newFixedThreadPool(10);
        IntStream.range(0, count)
            .forEach(i -> service.submit(() -> {


            }));*//*

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private int getRandomNumberInRange(int min, int max) {
        int x = (int)(Math.random()*((max-min)+1))+min;
        return x;
    }*/
}
