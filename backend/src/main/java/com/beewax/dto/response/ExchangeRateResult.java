package com.beewax.dto.response;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ExchangeRateResult {

	private static final String FAILURE_MESSAGE = "汇率获取失败，请手动输入";

	private final boolean success;
	private final BigDecimal rate;
	private final LocalDate date;
	private final String source;
	private final String message;

	private ExchangeRateResult(boolean success, BigDecimal rate, LocalDate date, String source, String message) {
		this.success = success;
		this.rate = rate;
		this.date = date;
		this.source = source;
		this.message = message;
	}

	public static ExchangeRateResult success(LocalDate date, BigDecimal rate, String source) {
		return new ExchangeRateResult(true, rate, date, source, null);
	}

	public static ExchangeRateResult failure() {
		return new ExchangeRateResult(false, null, null, null, FAILURE_MESSAGE);
	}
}
