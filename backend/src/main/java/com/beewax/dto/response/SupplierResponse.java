package com.beewax.dto.response;

import com.beewax.entity.Supplier;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SupplierResponse {

	private Long id;
	private String name;
	private String contactName;
	private String contactInfo;
	private String remark;
	private String status;

	public static SupplierResponse from(Supplier supplier) {
		return new SupplierResponse(
				supplier.getId(),
				supplier.getName(),
				supplier.getContactName(),
				supplier.getContactInfo(),
				supplier.getRemark(),
				supplier.getStatus().name());
	}
}
