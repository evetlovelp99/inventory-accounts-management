package com.beewax.controller;

import com.beewax.dto.request.SupplierCreateRequest;
import com.beewax.dto.request.SupplierStatusRequest;
import com.beewax.dto.request.SupplierUpdateRequest;
import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.PageResponse;
import com.beewax.dto.response.SupplierResponse;
import com.beewax.entity.Supplier.SupplierStatus;
import com.beewax.service.SupplierService;
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
@RequestMapping("/api/settings/suppliers")
public class SupplierController {

	private final SupplierService supplierService;

	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

	@GetMapping
	public ApiResponse<PageResponse<SupplierResponse>> listSuppliers(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) SupplierStatus status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(supplierService.listSuppliers(keyword, status, page, size));
	}

	@PostMapping
	public ApiResponse<SupplierResponse> createSupplier(@Valid @RequestBody SupplierCreateRequest request) {
		return ApiResponse.ok(supplierService.createSupplier(request));
	}

	@PutMapping("/{id}")
	public ApiResponse<SupplierResponse> updateSupplier(
			@PathVariable Long id,
			@Valid @RequestBody SupplierUpdateRequest request) {
		return ApiResponse.ok(supplierService.updateSupplier(id, request));
	}

	@PutMapping("/{id}/status")
	public ApiResponse<SupplierResponse> updateSupplierStatus(
			@PathVariable Long id,
			@Valid @RequestBody SupplierStatusRequest request) {
		return ApiResponse.ok(supplierService.updateSupplierStatus(id, request));
	}
}
