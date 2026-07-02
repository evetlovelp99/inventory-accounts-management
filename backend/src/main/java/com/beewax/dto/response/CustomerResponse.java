package com.beewax.dto.response;

import com.beewax.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerResponse {

	private Long id;
	private String name;
	private String country;
	private String contactName;
	private String contactInfo;
	private String remark;
	private String status;

	public static CustomerResponse from(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.getName(),
				customer.getCountry(),
				customer.getContactName(),
				customer.getContactInfo(),
				customer.getRemark(),
				customer.getStatus().name());
	}
}
