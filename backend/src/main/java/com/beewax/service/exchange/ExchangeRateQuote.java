package com.beewax.service.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateQuote(LocalDate date, BigDecimal rate, String source) {
}
