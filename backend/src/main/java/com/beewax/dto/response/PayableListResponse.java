package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class PayableListResponse {

	private BigDecimal totalUnpaidAmount;
	private List<PayableSummaryItemResponse> list;
	private long total;
}
