package com.github.wikor2115.reservation.availability.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wikor2115.reservation.availability.service.AvailabilitySlotNotFoundException;
import com.github.wikor2115.reservation.availability.service.DuplicateAvailabilitySlotException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AvailabilitySlotNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAvailabilitySlotNotFound(
            AvailabilitySlotNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        "AVAILABILITY_SLOT_NOT_FOUND",
                        exception.getMessage(),
                        List.of(new ApiFieldError("slotId", exception.getMessage()))
                ));
    }

    @ExceptionHandler(DuplicateAvailabilitySlotException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateAvailabilitySlot(
            DuplicateAvailabilitySlotException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        "AVAILABILITY_SLOT_ALREADY_EXISTS",
                        exception.getMessage(),
                        List.of(new ApiFieldError("timeRange", exception.getMessage()))
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
