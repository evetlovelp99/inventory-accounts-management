package com.beewax.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierUpdateRequest {

	@NotBlank(message = "供应商名称不能为空")
	private String name;

	private String contactName;

	private String contactInfo;

	private String remark;
}
