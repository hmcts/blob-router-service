package uk.gov.hmcts.reform.blobrouter.exceptionhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.blobrouter.exceptions.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.blobrouter.model.out.ErrorResponse;

@RestControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    @ExceptionHandler(UnableToGenerateSasTokenException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleUnableToGenerateSasTokenException(UnableToGenerateSasTokenException exception) {
        return new ErrorResponse("Exception occurred while generating SAS Token", exception.getClass());
    }

    @ExceptionHandler(ServiceConfigNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleServiceConfigNotFoundException(ServiceConfigNotFoundException exception) {
        return new ErrorResponse(exception.getMessage(), exception.getClass());
    }

    @ExceptionHandler(ServiceDisabledException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleServiceDisabledException(ServiceDisabledException exception) {
        return new ErrorResponse(exception.getMessage(), exception.getClass());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleInternalException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return new ErrorResponse(exception.getMessage(), exception.getClass());
    }
}
