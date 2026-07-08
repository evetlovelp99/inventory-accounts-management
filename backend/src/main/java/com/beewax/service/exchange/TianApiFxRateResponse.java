package com.beewax.service.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class TianApiFxRateResponse {

	private int code;

	private String msg;

	private Result result;

	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown = true)
	static class Result {

		private String money;
	}
}
