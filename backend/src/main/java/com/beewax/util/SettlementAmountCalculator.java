package com.beewax.util;

import com.beewax.entity.SettlementCurrency;
import com.beewax.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SettlementAmountCalculator {

	private SettlementAmountCalculator() {
	}

	public static BigDecimal calculateConvertedAmount(
			SettlementCurrency currency,
			BigDecimal originalAmount,
			BigDecimal exchangeRate) {
		if (currency == SettlementCurrency.USD) {
			if (exchangeRate == null) {
				throw new BusinessException(400, "请填写汇率");
			}
			BigDecimal normalizedRate = exchangeRate.setScale(4, RoundingMode.HALF_UP);
			return originalAmount.multiply(normalizedRate).setScale(2, RoundingMode.HALF_UP);
		}
		return originalAmount.setScale(2, RoundingMode.HALF_UP);
	}
}
