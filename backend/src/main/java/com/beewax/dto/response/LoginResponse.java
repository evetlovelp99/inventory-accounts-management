package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

	private final String token;
	private final UserInfoResponse user;
}
