package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@Api(tags = {"Bulk Scanning Payment API"})
@SwaggerDefinition(tags = {@Tag(name = "BulkScanningController",
    description = "Bulk Scanning Payment API to be used by the scanning supplier to share the "
        + "payment information contained in the envelope")})
public class BulkScanningController {

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to ccpay-bulkscanning-app");
    }

}
