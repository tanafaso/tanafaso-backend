package com.azkar.configs;

import com.azkar.payload.ResponseBase.Error;
import com.azkar.payload.exceptions.DefaultExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlingConfig {

  @ExceptionHandler
  public ResponseEntity<DefaultExceptionResponse> handleException(Exception e) {
    DefaultExceptionResponse response = new DefaultExceptionResponse();
    response.setError(new Error(DefaultExceptionResponse.DEFAULT_ERROR));
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

}
