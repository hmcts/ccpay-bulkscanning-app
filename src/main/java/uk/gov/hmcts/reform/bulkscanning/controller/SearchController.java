package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.service.SearchService;

import java.util.Optional;

@RestController
@Tag(name = "Bulk Scanning Payment Search API",description = "Bulk Scanning Payment Search APIs to be used by Pay-Bubble to retrieve all unprocessed payments")
    public class SearchController {

    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);
    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "Case with unprocessed payments details by CCD Case Reference/Exception Record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payments retrieved"),
        @ApiResponse(responseCode = "404", description = "Payments not found")
    })
    @GetMapping("/cases/{ccd_reference}")
    public ResponseEntity retrieveByCCD(
        @RequestHeader("Authorization") String authorization,
        @PathVariable("ccd_reference") String ccdReference) {
        LOG.info("Retrieving payments for ccdReference {} : ", ccdReference);
        try {
            SearchResponse searchResponse = searchService.retrieveByCCDReference(ccdReference);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                LOG.info("SearchResponse : {}", searchResponse);
                return ResponseEntity.status(HttpStatus.OK).body(searchResponse);
            } else {
                LOG.info("Payments Not found for ccdReference : {}", ccdReference);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payments Not found for ccdReference");
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @Operation(summary = "Case with unprocessed payment details by Payment DCN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payments retrieved"),
        @ApiResponse(responseCode = "404", description = "Payments not found")
    })
    @GetMapping("/cases")
    public ResponseEntity retrieveByDCN(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("document_control_number") String documentControlNumber) {
            return retrieveByDCN(documentControlNumber, false);
    }

    @Operation(summary = "Case with unprocessed payment details by Payment DCN (invoked by payment app)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payments retrieved"),
            @ApiResponse(responseCode = "404", description = "Payments not found")
    })
    @GetMapping("/case/{document_control_number}")
    public ResponseEntity retrieveByDCN(
            @PathVariable("document_control_number") String documentControlNumber,
            @RequestParam(defaultValue = "true") boolean internalFlag) {
        LOG.info("Retrieving payments for documentControlNumber : {}", documentControlNumber);
        try {
            SearchResponse searchResponse = searchService.retrieveByDcn(documentControlNumber, internalFlag);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(searchResponse);
            } else {
                LOG.info("Payments Not found for documentControlNumber : {}", documentControlNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payments Not found for documentControlNumber");
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }
}
