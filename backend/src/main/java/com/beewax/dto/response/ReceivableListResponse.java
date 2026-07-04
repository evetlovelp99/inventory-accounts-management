package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class ReceivableListResponse {

	private BigDecimal totalUnpaidAmount;
	private List<ReceivableSummaryItemResponse> list;
	private long total;
}
