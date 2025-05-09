package com.siemens.internship.exception;


import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.stream.Collectors;

// Enables centralized exception handling across controllers
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles validation errors from @Valid annotated @RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        // Format validation error messages into a readable string
        String errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
