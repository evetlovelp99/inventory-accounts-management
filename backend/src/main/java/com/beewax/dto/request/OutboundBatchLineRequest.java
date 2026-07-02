package com.beewax.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OutboundBatchLineRequest {

	@NotNull(message = "入库批次不能为空")
	private Long inboundId;

	@NotNull(message = "出库数量不能为空")
	@Positive(message = "出库数量必须大于0")
	private BigDecimal qty;
}
