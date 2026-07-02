package com.beewax.controller;

import com.beewax.dto.request.CustomerCreateRequest;
import com.beewax.dto.request.CustomerStatusRequest;
import com.beewax.dto.request.CustomerUpdateRequest;
import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.CustomerResponse;
import com.beewax.dto.response.PageResponse;
import com.beewax.entity.Customer.CustomerStatus;
import com.beewax.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping
	public ApiResponse<PageResponse<CustomerResponse>> listCustomers(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) CustomerStatus status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(customerService.listCustomers(keyword, status, page, size));
	}

	@PostMapping
	public ApiResponse<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
		return ApiResponse.ok(customerService.createCustomer(request));
	}

	@PutMapping("/{id}")
	public ApiResponse<CustomerResponse> updateCustomer(
			@PathVariable Long id,
			@Valid @RequestBody CustomerUpdateRequest request) {
		return ApiResponse.ok(customerService.updateCustomer(id, request));
	}

	@PutMapping("/{id}/status")
	public ApiResponse<CustomerResponse> updateCustomerStatus(
			@PathVariable Long id,
			@Valid @RequestBody CustomerStatusRequest request) {
		return ApiResponse.ok(customerService.updateCustomerStatus(id, request));
	}
}
