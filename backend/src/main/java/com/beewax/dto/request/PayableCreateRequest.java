package com.beewax.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PayableCreateRequest {

	@NotNull(message = "供应商不能为空")
	private Long supplierId;

	@NotNull(message = "应付金额不能为空")
	@DecimalMin(value = "0.01", message = "应付金额必须大于0")
	private BigDecimal originalAmount;

	@NotNull(message = "发生日期不能为空")
	private LocalDate occurDate;

	private Long inboundId;

	private String remark;
}
