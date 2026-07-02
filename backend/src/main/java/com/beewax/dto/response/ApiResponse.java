package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

	private final int code;
	private final String message;
	private final T data;

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(200, "ok", data);
	}

	public static <T> ApiResponse<T> error(int code, String message) {
		return new ApiResponse<>(code, message, null);
	}
}
