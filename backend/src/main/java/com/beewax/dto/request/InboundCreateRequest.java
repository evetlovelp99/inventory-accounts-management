package com.beewax.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class InboundCreateRequest {

	@NotNull(message = "产品不能为空")
	private Long productId;

	@NotNull(message = "供应商不能为空")
	private Long supplierId;

	@NotNull(message = "入库日期不能为空")
	private LocalDate inboundDate;

	@NotNull(message = "数量不能为空")
	@Positive(message = "数量必须大于0")
	private BigDecimal quantity;

	@NotNull(message = "单价不能为空")
	@DecimalMin(value = "0", message = "单价不能为负")
	private BigDecimal unitPrice;

	private String remark;

	private Boolean createPayable;

	private String originPlace;

	private LocalDate harvestDate;

	private String inspectNo;

	private String inspectOrg;

	private LocalDate inspectDate;

	private String inspectFileUrl;

	private LocalDate expiryDate;
}
