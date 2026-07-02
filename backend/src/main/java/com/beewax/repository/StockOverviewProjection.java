package com.beewax.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface StockOverviewProjection {

	Long getProductId();

	String getProductName();

	String getSpec();

	String getUnit();

	BigDecimal getTotalRemaining();

	LocalDateTime getLastUpdated();
}
