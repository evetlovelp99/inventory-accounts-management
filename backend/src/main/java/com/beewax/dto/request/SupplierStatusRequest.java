package com.beewax.dto.request;

import com.beewax.entity.Supplier.SupplierStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierStatusRequest {

	@NotNull(message = "状态不能为空")
	private SupplierStatus status;

	private Boolean force;
}
