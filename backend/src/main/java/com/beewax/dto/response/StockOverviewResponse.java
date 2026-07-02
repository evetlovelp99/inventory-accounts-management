package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class StockOverviewResponse {

	private Long productId;
	private String productName;
	private String spec;
	private String unit;
	private BigDecimal totalRemaining;
	private LocalDate lastUpdated;
}
