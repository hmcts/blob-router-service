package uk.gov.hmcts.reform.blobrouter.exceptionhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.blobrouter.model.out.ErrorResponse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.status;

@RestControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    @ExceptionHandler(UnableToGenerateSasTokenException.class)
    protected ResponseEntity<ErrorResponse> handleUnableToGenerateSasTokenException() {
        return status(INTERNAL_SERVER_ERROR).body(new ErrorResponse("Exception occurred while generating SAS Token"));
    }

    @ExceptionHandler(ServiceConfigNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleServiceConfigNotFoundException(ServiceConfigNotFoundException e) {
        return status(BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Void> handleInternalException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
