package com.beewax.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerUpdateRequest {

	@NotBlank(message = "客户名称不能为空")
	private String name;

	private String country;

	private String contactName;

	private String contactInfo;

	private String remark;
}
