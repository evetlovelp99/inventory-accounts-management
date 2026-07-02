package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class InboundCreateResponse {

	private Long inboundId;
	private BigDecimal totalAmount;
	private Long payableId;
}
