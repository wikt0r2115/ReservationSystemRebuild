package com.github.wikor2115.reservation.auth.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wikor2115.reservation.auth.service.DuplicateUserAccountException;
import com.github.wikor2115.reservation.auth.service.InvalidCredentialsException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUserAccountException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateUserAccount(DuplicateUserAccountException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        "USER_ACCOUNT_ALREADY_EXISTS",
                        exception.getMessage(),
                        List.of(new ApiFieldError("email", exception.getMessage()))
                ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        "AUTHENTICATION_FAILED",
                        exception.getMessage(),
                        List.of(new ApiFieldError("credentials", exception.getMessage()))
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "VALIDATION_ERROR",
                        exception.getMessage(),
                        exception.getBindingResult().getFieldErrors().stream()
                                .map(fieldError -> new ApiFieldError(
                                        fieldError.getField(),
                                        fieldError.getDefaultMessage()))
                                .collect(Collectors.toList())
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "INVALID_REQUEST_BODY",
                        "Request body is not readable",
                        List.of(new ApiFieldError("body", "Malformed JSON or invalid field type"))
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(
                        "ACCESS_DENIED",
                        exception.getMessage(),
                        List.of(new ApiFieldError("authorization", exception.getMessage()))
                ));
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "BUSINESS_RULE_VIOLATION",
                        exception.getMessage(),
                        List.of(new ApiFieldError("message", exception.getMessage()))
                ));
    }
}
