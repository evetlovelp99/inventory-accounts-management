package com.beewax.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ReceivableSummaryProjection {

	Long getCustomerId();

	String getCustomerName();

	String getCurrency();

	BigDecimal getOriginalAmount();

	BigDecimal getConvertedAmount();

	BigDecimal getPaidAmount();

	BigDecimal getRemainingAmount();

	LocalDate getOldestUnpaidDate();

	Integer getDaysSinceOldest();

	String getStatus();
}
