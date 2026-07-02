package com.beewax.dto.request;

import com.beewax.entity.Product.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusRequest {

	@NotNull(message = "状态不能为空")
	private ProductStatus status;

	private Boolean force;
}
