package com.beewax.controller;

import com.beewax.dto.request.InboundCreateRequest;
import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.InboundCreateResponse;
import com.beewax.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
