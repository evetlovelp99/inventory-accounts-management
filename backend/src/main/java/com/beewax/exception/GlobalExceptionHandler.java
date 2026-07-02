package com.beewax.exception;

import com.beewax.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
		return ResponseEntity.status(ex.getCode()).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
		int code = ex.getStatusCode().value();
		String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
		return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.error(code, message));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.findFirst()
				.orElse("参数错误");
		return ResponseEntity.status(400).body(ApiResponse.error(400, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.status(400).body(ApiResponse.error(400, "参数错误"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
		return ResponseEntity.status(500).body(ApiResponse.error(500, "服务器内部错误"));
	}
}
