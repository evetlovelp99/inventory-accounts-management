package com.beewax.controller;

import com.beewax.dto.request.PayableCreateRequest;
import com.beewax.dto.request.PayablePaymentRequest;
import com.beewax.dto.request.ReceivableCreateRequest;
import com.beewax.dto.request.ReceivablePaymentRequest;
import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.PayableCreateResponse;
import com.beewax.dto.response.PayableDetailResponse;
import com.beewax.dto.response.PayableListResponse;
import com.beewax.dto.response.PayablePaymentResponse;
import com.beewax.dto.response.ReceivableCreateResponse;
import com.beewax.dto.response.ReceivableDetailResponse;
import com.beewax.dto.response.ReceivableListResponse;
import com.beewax.dto.response.ReceivablePaymentResponse;
import com.beewax.entity.AccountPayable.PayableStatus;
import com.beewax.entity.AccountReceivable.ReceivableStatus;
import com.beewax.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounts")
public class AccountsController {

	private final AccountService accountService;

	public AccountsController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping("/receivable")
	public ApiResponse<ReceivableListResponse> listReceivables(
			@RequestParam(required = false) ReceivableStatus status,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(accountService.listReceivables(keyword, status, page, size));
	}

	@PostMapping("/receivable")
	public ApiResponse<ReceivableCreateResponse> createReceivable(
			@Valid @RequestBody ReceivableCreateRequest request) {
		return ApiResponse.ok(accountService.createReceivable(request));
	}

	@GetMapping("/receivable/detail")
	public ApiResponse<ReceivableDetailResponse> getReceivableDetail(
			@RequestParam Long customerId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ApiResponse.ok(accountService.getReceivableDetail(customerId, startDate, endDate));
	}

	@PostMapping("/receivable/{id}/payment")
	public ApiResponse<ReceivablePaymentResponse> registerReceivablePayment(
			@PathVariable Long id,
			@Valid @RequestBody ReceivablePaymentRequest request) {
		return ApiResponse.ok(accountService.registerReceivablePayment(id, request));
	}

	@GetMapping("/payable")
	public ApiResponse<PayableListResponse> listPayables(
			@RequestParam(required = false) PayableStatus status,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(accountService.listPayables(keyword, status, page, size));
	}

	@PostMapping("/payable")
	public ApiResponse<PayableCreateResponse> createPayable(
			@Valid @RequestBody PayableCreateRequest request) {
		return ApiResponse.ok(accountService.createPayable(request));
	}

	@GetMapping("/payable/detail")
	public ApiResponse<PayableDetailResponse> getPayableDetail(
			@RequestParam Long supplierId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ApiResponse.ok(accountService.getPayableDetail(supplierId, startDate, endDate));
	}

	@PostMapping("/payable/{id}/payment")
	public ApiResponse<PayablePaymentResponse> registerPayablePayment(
			@PathVariable Long id,
			@Valid @RequestBody PayablePaymentRequest request) {
		return ApiResponse.ok(accountService.registerPayablePayment(id, request));
	}
}
