package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.*;
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
@Api(tags = {"Bulk Scanning Payment Search API"})
@SwaggerDefinition(tags = {@Tag(name = "SearchController",
    description = "Bulk Scanning Payment Search APIs to be used by Pay-Bubble to retrieve all unprocessed payments")})
public class SearchController {

    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);
    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @ApiOperation("Case with unprocessed payments details by CCD Case Reference/Exception Record")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
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

    @ApiOperation("Case with unprocessed payment details by Payment DCN")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases")
    public ResponseEntity retrieveByDCN(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("document_control_number") String documentControlNumber) {
        LOG.info("Retrieving payments for documentControlNumber : {}", documentControlNumber);
        try {
            SearchResponse searchResponse = searchService.retrieveByDcn(documentControlNumber);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                LOG.info("SearchResponse : {}", searchResponse);
                return ResponseEntity.status(HttpStatus.OK).body(searchResponse);
            } else {
                LOG.info("Payments Not found for documentControlNumber : {}", documentControlNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payments Not found for documentControlNumber");
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @ApiOperation("Case with unprocessed payment details by Payment DCN (invoked by payment app)")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Payments retrieved"),
            @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases/dcn/{document_control_number}")
    public ResponseEntity retrieveByDCN(
            @PathVariable("document_control_number") String documentControlNumber) {
        LOG.info("Retrieving payments for documentControlNumber : {}", documentControlNumber);
        try {
            SearchResponse searchResponse = searchService.retrieveByDcn(documentControlNumber);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                LOG.info("SearchResponse : {}", searchResponse);
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
