package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ReceivablePaymentResponse {

	private BigDecimal remainingAmount;
	private String status;
}
