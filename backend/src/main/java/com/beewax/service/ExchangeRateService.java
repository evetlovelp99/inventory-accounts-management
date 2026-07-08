package com.beewax.service;

import com.beewax.dto.response.ExchangeRateResult;
import com.beewax.service.exchange.ExchangeRateProvider;
import com.beewax.service.exchange.ExchangeRateQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeRateService {

	private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

	private final ExchangeRateProvider exchangeRateProvider;
	private final Map<LocalDate, ExchangeRateResult> cache = new ConcurrentHashMap<>();

	public ExchangeRateService(ExchangeRateProvider exchangeRateProvider) {
		this.exchangeRateProvider = exchangeRateProvider;
	}

	public ExchangeRateResult getCnyUsdRate(LocalDate date) {
		LocalDate queryDate = date != null ? date : LocalDate.now();
		ExchangeRateResult cached = cache.get(queryDate);
		if (cached != null) {
			return cached;
		}

		try {
			Optional<ExchangeRateQuote> quote = exchangeRateProvider.fetchUsdCnyRate(queryDate);
			if (quote.isEmpty()) {
				return ExchangeRateResult.failure();
			}

			ExchangeRateQuote value = quote.get();
			ExchangeRateResult result = ExchangeRateResult.success(
					value.date(),
					value.rate(),
					value.source());
			cache.put(queryDate, result);
			return result;
		} catch (RuntimeException ex) {
			log.warn("Unexpected error while fetching exchange rate for date {}", queryDate, ex);
			return ExchangeRateResult.failure();
		}
	}
}
