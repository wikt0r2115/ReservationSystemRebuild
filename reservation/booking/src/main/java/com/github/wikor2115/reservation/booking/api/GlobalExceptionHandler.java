package com.github.wikor2115.reservation.booking.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.wikor2115.reservation.availability.service.AvailabilitySlotNotFoundException;
import com.github.wikor2115.reservation.booking.service.ReservationNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReservationNotFound(ReservationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        "RESERVATION_NOT_FOUND",
                        exception.getMessage(),
                        List.of(new ApiFieldError("reservationId", exception.getMessage()))
                ));
    }

    @ExceptionHandler(AvailabilitySlotNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAvailabilitySlotNotFound(
            AvailabilitySlotNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        "AVAILABILITY_SLOT_NOT_FOUND",
                        exception.getMessage(),
                        List.of(new ApiFieldError("availabilitySlotId", exception.getMessage()))
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
                        "RESERVATION_ACCESS_DENIED",
                        exception.getMessage(),
                        List.of(new ApiFieldError("customerEmail", exception.getMessage()))
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
