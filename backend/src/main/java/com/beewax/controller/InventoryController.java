package com.beewax.controller;

import com.beewax.dto.request.InboundCreateRequest;
import com.beewax.dto.request.OutboundCreateRequest;
import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.InboundBatchResponse;
import com.beewax.dto.response.InboundCreateResponse;
import com.beewax.dto.response.OutboundCreateResponse;
import com.beewax.dto.response.PageResponse;
import com.beewax.dto.response.StockOverviewResponse;
import com.beewax.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@PostMapping("/inbound")
	public ApiResponse<InboundCreateResponse> createInbound(@Valid @RequestBody InboundCreateRequest request) {
		return ApiResponse.ok(inventoryService.createInbound(request));
	}

	@GetMapping("/stock")
	public ApiResponse<PageResponse<StockOverviewResponse>> listStock(
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(inventoryService.listStock(keyword, page, size));
	}

	@GetMapping("/inbound/{productId}/batches")
	public ApiResponse<List<InboundBatchResponse>> listInboundBatches(@PathVariable Long productId) {
		return ApiResponse.ok(inventoryService.listInboundBatches(productId));
	}

	@PostMapping("/outbound")
	public ApiResponse<OutboundCreateResponse> createOutbound(@Valid @RequestBody OutboundCreateRequest request) {
		return ApiResponse.ok(inventoryService.createOutbound(request));
	}
}
