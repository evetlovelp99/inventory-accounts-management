package com.beewax.service;

import com.beewax.dto.response.PaymentLogResponse;
import com.beewax.dto.response.ReceivableDetailResponse;
import com.beewax.dto.response.ReceivableListResponse;
import com.beewax.dto.response.ReceivableRecordResponse;
import com.beewax.dto.response.ReceivableSummaryItemResponse;
import com.beewax.entity.AccountReceivable;
import com.beewax.entity.AccountReceivable.ReceivableStatus;
import com.beewax.entity.PaymentLog;
import com.beewax.entity.PaymentLog.AccountType;
import com.beewax.exception.BusinessException;
import com.beewax.repository.AccountReceivableRepository;
import com.beewax.repository.CustomerRepository;
import com.beewax.repository.PaymentLogRepository;
import com.beewax.repository.ReceivableSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

	private final AccountReceivableRepository accountReceivableRepository;
	private final PaymentLogRepository paymentLogRepository;
	private final CustomerRepository customerRepository;

	public AccountService(
			AccountReceivableRepository accountReceivableRepository,
			PaymentLogRepository paymentLogRepository,
			CustomerRepository customerRepository) {
		this.accountReceivableRepository = accountReceivableRepository;
		this.paymentLogRepository = paymentLogRepository;
		this.customerRepository = customerRepository;
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

	public ReceivableDetailResponse getReceivableDetail(Long customerId, LocalDate startDate, LocalDate endDate) {
		String customerName = customerRepository.findById(customerId)
				.orElseThrow(() -> new BusinessException(404, "客户不存在"))
				.getName();

		List<AccountReceivable> records = accountReceivableRepository.findByCustomerIdWithDateFilter(
				customerId, startDate, endDate);
		Map<Long, List<PaymentLogResponse>> paymentLogsByAccountId = loadPaymentLogs(records);

		List<ReceivableRecordResponse> recordResponses = records.stream()
				.map(record -> toRecordResponse(record, paymentLogsByAccountId.getOrDefault(record.getId(), List.of())))
				.toList();

		return new ReceivableDetailResponse(customerName, recordResponses);
	}

	private Map<Long, List<PaymentLogResponse>> loadPaymentLogs(List<AccountReceivable> records) {
		if (records.isEmpty()) {
			return Map.of();
		}

		List<Long> accountIds = records.stream().map(AccountReceivable::getId).toList();
		List<PaymentLog> paymentLogs = paymentLogRepository
				.findByAccountTypeAndAccountIdInOrderByPaymentDateDescIdDesc(AccountType.RECEIVABLE, accountIds);

		Map<Long, List<PaymentLogResponse>> grouped = new LinkedHashMap<>();
		for (PaymentLog log : paymentLogs) {
			grouped.computeIfAbsent(log.getAccountId(), ignored -> new ArrayList<>())
					.add(toPaymentLogResponse(log));
		}
		return grouped;
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

	private ReceivableRecordResponse toRecordResponse(AccountReceivable record, List<PaymentLogResponse> paymentLogs) {
		return new ReceivableRecordResponse(
				record.getId(),
				record.getOriginalAmount(),
				record.getPaidAmount(),
				record.getRemainingAmount(),
				record.getOccurDate(),
				record.getStatus().name(),
				record.getOutboundId(),
				record.getRemark(),
				paymentLogs);
	}

	private PaymentLogResponse toPaymentLogResponse(PaymentLog log) {
		return new PaymentLogResponse(log.getId(), log.getAmount(), log.getPaymentDate(), log.getRemark());
	}
}
