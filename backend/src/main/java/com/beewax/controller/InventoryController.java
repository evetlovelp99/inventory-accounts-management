package com.beewax.controller;

import com.beewax.dto.request.InboundCreateRequest;
import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.InboundBatchResponse;
import com.beewax.dto.response.InboundCreateResponse;
import com.beewax.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	@GetMapping("/inbound/{productId}/batches")
	public ApiResponse<List<InboundBatchResponse>> listInboundBatches(@PathVariable Long productId) {
		return ApiResponse.ok(inventoryService.listInboundBatches(productId));
	}
}
