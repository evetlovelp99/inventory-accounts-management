package com.beewax.controller;

import com.beewax.dto.request.ProductCreateRequest;
import com.beewax.dto.request.ProductStatusRequest;
import com.beewax.dto.request.ProductUpdateRequest;
import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.PageResponse;
import com.beewax.dto.response.ProductResponse;
import com.beewax.entity.Product.ProductStatus;
import com.beewax.service.ProductService;
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
@RequestMapping("/api/settings/products")
public class SettingsController {

	private final ProductService productService;

	public SettingsController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public ApiResponse<PageResponse<ProductResponse>> listProducts(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(productService.listProducts(keyword, status, page, size));
	}

	@PostMapping
	public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
		return ApiResponse.ok(productService.createProduct(request));
	}

	@PutMapping("/{id}")
	public ApiResponse<ProductResponse> updateProduct(
			@PathVariable Long id,
			@Valid @RequestBody ProductUpdateRequest request) {
		return ApiResponse.ok(productService.updateProduct(id, request));
	}

	@PutMapping("/{id}/status")
	public ApiResponse<ProductResponse> updateProductStatus(
			@PathVariable Long id,
			@Valid @RequestBody ProductStatusRequest request) {
		return ApiResponse.ok(productService.updateProductStatus(id, request));
	}
}
