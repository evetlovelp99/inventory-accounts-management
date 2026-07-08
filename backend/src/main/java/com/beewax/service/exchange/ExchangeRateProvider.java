package com.beewax.service.exchange;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateProvider {

	Optional<ExchangeRateQuote> fetchUsdCnyRate(LocalDate date);

	String getSourceCode();
}
