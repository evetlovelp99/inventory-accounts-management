package com.beewax.service;

import com.beewax.dto.response.ReceivableListResponse;
import com.beewax.dto.response.ReceivableSummaryItemResponse;
import com.beewax.entity.AccountReceivable.ReceivableStatus;
import com.beewax.repository.AccountReceivableRepository;
import com.beewax.repository.ReceivableSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

	private final AccountReceivableRepository accountReceivableRepository;

	public AccountService(AccountReceivableRepository accountReceivableRepository) {
		this.accountReceivableRepository = accountReceivableRepository;
	}

	public ReceivableListResponse listReceivables(String keyword, ReceivableStatus status, int page, int size) {
		String searchKeyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
		String statusFilter = status != null ? status.name() : null;

		BigDecimal totalUnpaidAmount = accountReceivableRepository.sumRemainingAmount(searchKeyword, statusFilter);
		Page<ReceivableSummaryProjection> result = accountReceivableRepository.findReceivableSummary(
				searchKeyword, statusFilter, PageRequest.of(page - 1, size));

		List<ReceivableSummaryItemResponse> list = result.getContent().stream()
				.map(this::toSummaryItem)
				.toList();

		return new ReceivableListResponse(totalUnpaidAmount, list, result.getTotalElements());
	}

	private ReceivableSummaryItemResponse toSummaryItem(ReceivableSummaryProjection row) {
		return new ReceivableSummaryItemResponse(
				row.getCustomerId(),
				row.getCustomerName(),
				row.getOriginalAmount(),
				row.getPaidAmount(),
				row.getRemainingAmount(),
				row.getOldestUnpaidDate(),
				row.getDaysSinceOldest() != null ? row.getDaysSinceOldest() : 0,
				row.getStatus());
	}
}
