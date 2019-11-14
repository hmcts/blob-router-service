package uk.gov.hmcts.reform.blobrouter.exceptionhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.blobrouter.model.out.ErrorResponse;

@RestControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    @ExceptionHandler(UnableToGenerateSasTokenException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleUnableToGenerateSasTokenException(UnableToGenerateSasTokenException e) {
        return new ErrorResponse("Exception occurred while generating SAS Token", e.getCause());
    }

    @ExceptionHandler(ServiceConfigNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleServiceConfigNotFoundException(ServiceConfigNotFoundException e) {
        return new ErrorResponse(e.getMessage(), e.getCause());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleInternalException(Exception e) {
        log.error(e.getMessage(), e);
        return new ErrorResponse(e.getMessage(), e.getCause());
    }
}
