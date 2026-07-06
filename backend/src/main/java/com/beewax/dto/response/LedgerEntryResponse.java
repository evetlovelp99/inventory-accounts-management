package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class LedgerEntryResponse {

	private Long id;
	private String type;
	private LocalDate date;
	private BigDecimal qty;
	private BigDecimal unitPrice;
	private BigDecimal amount;
	private String partyName;
	private String remark;
	private InboundProductionInfoResponse productionInfo;
}
