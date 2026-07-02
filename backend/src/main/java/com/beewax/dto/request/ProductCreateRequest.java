package com.beewax.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequest {

	@NotBlank(message = "产品名称不能为空")
	private String name;

	private String spec;

	@NotBlank(message = "计量单位不能为空")
	private String unit;
}
