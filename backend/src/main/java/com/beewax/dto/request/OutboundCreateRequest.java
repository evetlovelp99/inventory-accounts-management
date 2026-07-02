package com.beewax.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class OutboundCreateRequest {

	@NotNull(message = "产品不能为空")
	private Long productId;

	@NotNull(message = "客户不能为空")
	private Long customerId;

	@NotNull(message = "出库日期不能为空")
	private LocalDate outboundDate;

	@NotNull(message = "销售单价不能为空")
	@DecimalMin(value = "0", message = "销售单价不能为负")
	private BigDecimal saleUnitPrice;

	private String remark;

	private Boolean createReceivable;

	@NotEmpty(message = "请至少添加一行批次明细")
	@Valid
	private List<OutboundBatchLineRequest> batchLines;
}
