package uk.gov.hmcts.reform.blobrouter.exceptionhandler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeCompletedOrNotStaleException;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidApiKeyException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidRequestParametersException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSupplierStatementException;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.blobrouter.model.out.ErrorResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    @ExceptionHandler(UnableToGenerateSasTokenException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleUnableToGenerateSasTokenException(UnableToGenerateSasTokenException exception) {
        return new ErrorResponse("Exception occurred while generating SAS Token");
    }

    @ExceptionHandler(ServiceConfigNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleServiceConfigNotFoundException(ServiceConfigNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(InvalidRequestParametersException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleInvalidRequestParametersException(InvalidRequestParametersException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(InvalidSupplierStatementException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleInvalidSupplierStatementException(InvalidSupplierStatementException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    protected ErrorResponse handleInvalidApiKeyException(InvalidApiKeyException exception) {
        log.error(exception.getMessage(), exception);
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(EnvelopeCompletedOrNotStaleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    protected ErrorResponse handleEnvelopeCompletedOrNotStaleException(EnvelopeCompletedOrNotStaleException exception) {
        log.error(exception.getMessage(), exception);
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(EnvelopeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected ErrorResponse handleEnvelopeNotFoundException(EnvelopeNotFoundException exception) {
        log.error(exception.getMessage(), exception);
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleInternalException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Map<String, String>> handleInternalException(ConstraintViolationException exception) {
        log.error(exception.getMessage(), exception);
        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();

        for (ConstraintViolation<?> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }
        return ResponseEntity.badRequest().body(errors);
    }
}
