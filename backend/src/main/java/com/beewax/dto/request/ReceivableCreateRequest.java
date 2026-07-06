package com.beewax.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ReceivableCreateRequest {

	@NotNull(message = "客户不能为空")
	private Long customerId;

	@NotNull(message = "应收金额不能为空")
	@DecimalMin(value = "0.01", message = "应收金额必须大于0")
	private BigDecimal originalAmount;

	@NotNull(message = "发生日期不能为空")
	private LocalDate occurDate;

	private Long outboundId;

	private String remark;
}
