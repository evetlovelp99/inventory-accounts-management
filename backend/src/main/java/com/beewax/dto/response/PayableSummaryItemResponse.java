package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class PayableSummaryItemResponse {

	private Long supplierId;
	private String supplierName;
	private BigDecimal originalAmount;
	private BigDecimal paidAmount;
	private BigDecimal remainingAmount;
	private LocalDate oldestUnpaidDate;
	private int daysSinceOldest;
	private String status;
}
