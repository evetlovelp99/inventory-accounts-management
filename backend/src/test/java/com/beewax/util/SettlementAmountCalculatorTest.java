package com.beewax.util;

import com.beewax.entity.SettlementCurrency;
import com.beewax.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementAmountCalculatorTest {

	@Test
	void calculateConvertedAmount_cny_returnsOriginalAmount() {
		BigDecimal original = new BigDecimal("1260.00");

		BigDecimal converted = SettlementAmountCalculator.calculateConvertedAmount(
				SettlementCurrency.CNY, original, null);

		assertEquals(new BigDecimal("1260.00"), converted);
	}

	@Test
	void calculateConvertedAmount_usd_multipliesByExchangeRate() {
		BigDecimal original = new BigDecimal("1260.00");
		BigDecimal rate = new BigDecimal("7.1523");

		BigDecimal converted = SettlementAmountCalculator.calculateConvertedAmount(
				SettlementCurrency.USD, original, rate);

		assertEquals(new BigDecimal("9011.90"), converted);
	}

	@Test
	void calculateConvertedAmount_usd_requiresExchangeRate() {
		assertThrows(BusinessException.class, () -> SettlementAmountCalculator.calculateConvertedAmount(
				SettlementCurrency.USD, new BigDecimal("100.00"), null));
	}
}
