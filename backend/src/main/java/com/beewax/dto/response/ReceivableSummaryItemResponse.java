package com.beewax.dto.response;

import com.beewax.entity.SettlementCurrency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ReceivableSummaryItemResponse {

	private Long customerId;
	private String customerName;
	private SettlementCurrency currency;
	private BigDecimal originalAmount;
	private BigDecimal convertedAmount;
	private BigDecimal paidAmount;
	private BigDecimal remainingAmount;
	private LocalDate oldestUnpaidDate;
	private int daysSinceOldest;
	private String status;
}
