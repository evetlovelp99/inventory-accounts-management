package com.beewax.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PayablePaymentRequest {

	@NotNull(message = "付款金额不能为空")
	@DecimalMin(value = "0.01", message = "付款金额必须大于0")
	private BigDecimal amount;

	@NotNull(message = "付款日期不能为空")
	private LocalDate paymentDate;

	private String remark;
}
