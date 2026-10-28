package com.motoservice.controller;

import com.motoservice.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = ApiController.class)
public class ApiErrors {
  private ResponseEntity<Map<String, Object>> error(
      int status, String message, HttpServletRequest req) {
    return ResponseEntity.status(status)
        .body(Map.of("status", status, "message", message, "path", req.getRequestURI()));
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<?> business(BusinessException ex, HttpServletRequest req) {
    return error(ex.status().value(), ex.getMessage(), req);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<?> invalid(Exception ex, HttpServletRequest req) {
    return error(400, "Invalid request: check required fields, types and lengths", req);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> conflict(Exception ex, HttpServletRequest req) {
    return error(409, "Record conflicts with an existing record", req);
  }

  @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
  public ResponseEntity<?> busy(Exception ex, HttpServletRequest req) {
    return error(409, "Slot is busy; refresh and retry", req);
  }
}
