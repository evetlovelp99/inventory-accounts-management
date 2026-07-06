package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class PaymentLogResponse {

	private Long id;
	private BigDecimal amount;
	private LocalDate paymentDate;
	private String remark;
}
