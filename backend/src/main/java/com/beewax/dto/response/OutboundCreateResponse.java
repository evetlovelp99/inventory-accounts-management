package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OutboundCreateResponse {

	private Long outboundId;
	private BigDecimal totalQty;
	private BigDecimal totalSaleAmount;
	private BigDecimal weightedCost;
	private BigDecimal grossProfit;
	private Long receivableId;
}
