package uk.gov.hmcts.reform.bulkscanning.validator;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.bulkscanning.audit.AppInsightsAuditRepository;
import uk.gov.hmcts.reform.bulkscanning.exception.BulkScanCaseAlreadyExistsException;
import uk.gov.hmcts.reform.bulkscanning.exception.DcnNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.exception.ExceptionRecordNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;

import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.DCN_NOT_EXISTS;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.EXCEPTION_RECORD_NOT_EXISTS;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils.asJsonString;


@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.bulkscanning.controller")
public class BulkScanControllerValidator extends ResponseEntityExceptionHandler {

    private final AppInsightsAuditRepository auditRepository;
    private static final Logger LOG = LoggerFactory.getLogger(BulkScanControllerValidator.class);

    public BulkScanControllerValidator(AppInsightsAuditRepository auditRepository) {
        super();
        this.auditRepository = auditRepository;
    }

    @ExceptionHandler(BulkScanCaseAlreadyExistsException.class)
    public ResponseEntity bsPaymentAlreadyExists(BulkScanCaseAlreadyExistsException bsAlreadyExistsException) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(asJsonString(bsAlreadyExistsException.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity handleConstrainVialoationException(ConstraintViolationException constraintViolationException) {

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(constraintViolationException.getMessage());
    }

    @ExceptionHandler(ExceptionRecordNotExistsException.class)
    public ResponseEntity exceptionRecordsNotExists(ExceptionRecordNotExistsException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(asJsonString(EXCEPTION_RECORD_NOT_EXISTS));
    }

    @ExceptionHandler(DcnNotExistsException.class)
    public ResponseEntity dcnNotExists(DcnNotExistsException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(asJsonString(DCN_NOT_EXISTS));

    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity handlePaymentException(PaymentException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {

        Map<String, Object> body = new ConcurrentHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());

        //Get all errors
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError:: getDefaultMessage)
            .collect(Collectors.toList());

        body.put("errors", errors);
        LOG.error("Error_Response : {}", errors);

        Map<String, String> errorMap = new ConcurrentHashMap<>();
        errorMap.put("errors", errors.toString());
        auditRepository.trackEvent("Error_Response", errorMap);

        return new ResponseEntity<>(body, headers, status);

    }

    @Override
    public ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

        String exceptionMessage = null;

        Throwable rootCause = ex.getRootCause();
        if(rootCause instanceof UnrecognizedPropertyException)
        {
            exceptionMessage = "Unknown field: " + ((UnrecognizedPropertyException) rootCause).getPropertyName();
            logger.debug("exceptionMessage: " + exceptionMessage);
        }

        Map<String, Object> body = new ConcurrentHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", exceptionMessage);

        return new ResponseEntity<Object>(body, new HttpHeaders(), status);
    }


}
