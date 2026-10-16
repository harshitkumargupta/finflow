package com.finflow.exception;

import com.finflow.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(
            ResourceAlreadyExistsException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "CONFLICT",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INSUFFICIENT_BALANCE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InsufficientAvailableBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAvailableBalance(
            InsufficientAvailableBalanceException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INSUFFICIENT_AVAILABLE_BALANCE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(SpendingLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleSpendingLimitExceeded(
            SpendingLimitExceededException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "SPENDING_LIMIT_EXCEEDED",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidMoneyRuleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMoneyRule(
            InvalidMoneyRuleException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_MONEY_RULE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ProviderNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleProviderNotConfigured(
            ProviderNotConfiguredException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PAYMENT_PROVIDER_UNAVAILABLE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(
            TooManyRequestsException exception,
            HttpServletRequest request) {

        ResponseEntity<ErrorResponse> response = buildResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "TOO_MANY_REQUESTS",
                exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(response.getStatusCode())
                .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
                .body(response.getBody());
    }

    @ExceptionHandler(InvalidPageRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPageRequest(
            InvalidPageRequestException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PAGE_REQUEST",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransfer(
            InvalidTransferException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_TRANSFER",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountInactive(
            AccountInactiveException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "ACCOUNT_INACTIVE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
            IdempotencyConflictException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidReceiptException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReceipt(
            InvalidReceiptException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_RECEIPT",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "PAYLOAD_TOO_LARGE",
                "The uploaded image is too large.",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI()
        );

        response.setErrors(errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path) {

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}