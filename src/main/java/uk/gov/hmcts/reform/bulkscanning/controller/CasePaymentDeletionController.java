package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanning.service.CasePaymentDeletionService;

@RestController
@Validated
@Tag(name = "Bulk Scanning Payment Deletion API",
    description = "API to delete bulk scan payment data by CCD case reference for retain and dispose")
public class CasePaymentDeletionController {

    private static final Logger LOG = LoggerFactory.getLogger(CasePaymentDeletionController.class);

    private final CasePaymentDeletionService casePaymentDeletionService;

    public CasePaymentDeletionController(CasePaymentDeletionService casePaymentDeletionService) {
        this.casePaymentDeletionService = casePaymentDeletionService;
    }

    @Operation(summary = "Delete bulk scan payment data by CCD case reference")
    @ApiResponse(responseCode = "204", description = "Bulk scan payment data deleted successfully, or no content found")
    @ApiResponse(responseCode = "400", description = "Invalid CCD case reference")
    @ApiResponse(responseCode = "403", description = "Failed service or user authorisation")
    @ApiResponse(responseCode = "500", description = "Unexpected error occurred during deletion")
    @DeleteMapping("/cases/{ccd-case-number}/bulk-scan-payments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByCcdCaseReference(
        @PathVariable("ccd-case-number")
        @NotNull
        @Size(min = 16, max = 16, message = "ccd-case-number Length must be 16 Characters")
        @Pattern(regexp = "\\d{16}", message = "ccd-case-number must be a 16 digit numeric value")
        String ccdCaseNumber) {

        LOG.info("Request received to delete bulk scan payments for CCD case reference: {}", ccdCaseNumber);
        casePaymentDeletionService.deleteByCcdCaseReference(ccdCaseNumber);
    }
}
