package com.beewax.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PayableSummaryProjection {

	Long getSupplierId();

	String getSupplierName();

	BigDecimal getOriginalAmount();

	BigDecimal getPaidAmount();

	BigDecimal getRemainingAmount();

	LocalDate getOldestUnpaidDate();

	Integer getDaysSinceOldest();

	String getStatus();
}
