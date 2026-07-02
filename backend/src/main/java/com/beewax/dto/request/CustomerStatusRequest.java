package com.beewax.dto.request;

import com.beewax.entity.Customer.CustomerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerStatusRequest {

	@NotNull(message = "状态不能为空")
	private CustomerStatus status;

	private Boolean force;
}
