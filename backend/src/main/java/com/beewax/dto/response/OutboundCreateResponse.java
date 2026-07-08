package com.beewax.dto.response;

import com.beewax.entity.SettlementCurrency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OutboundCreateResponse {

	private Long outboundId;
	private BigDecimal totalQty;
	private SettlementCurrency currency;
	private BigDecimal totalSaleAmount;
	private BigDecimal convertedSaleAmount;
	private BigDecimal weightedCost;
	private BigDecimal grossProfit;
	private Long receivableId;
}
