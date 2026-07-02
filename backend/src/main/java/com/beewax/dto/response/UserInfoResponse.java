package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoResponse {

	private final Long id;
	private final String name;
	private final String role;
}
