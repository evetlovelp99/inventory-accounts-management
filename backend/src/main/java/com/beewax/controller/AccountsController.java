package com.beewax.controller;

import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.ReceivableDetailResponse;
import com.beewax.dto.response.ReceivableListResponse;
import com.beewax.entity.AccountReceivable.ReceivableStatus;
import com.beewax.service.AccountService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
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

	@GetMapping("/receivable/detail")
	public ApiResponse<ReceivableDetailResponse> getReceivableDetail(
			@RequestParam Long customerId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ApiResponse.ok(accountService.getReceivableDetail(customerId, startDate, endDate));
	}
}
