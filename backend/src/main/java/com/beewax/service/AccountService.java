package com.beewax.service;

import com.beewax.config.JwtAuthenticationFilter.JwtPrincipal;
import com.beewax.dto.request.ReceivablePaymentRequest;
import com.beewax.dto.response.PaymentLogResponse;
import com.beewax.dto.response.ReceivableDetailResponse;
import com.beewax.dto.response.ReceivableListResponse;
import com.beewax.dto.response.ReceivablePaymentResponse;
import com.beewax.dto.response.ReceivableRecordResponse;
import com.beewax.dto.response.ReceivableSummaryItemResponse;
import com.beewax.entity.AccountReceivable;
import com.beewax.entity.AccountReceivable.ReceivableStatus;
import com.beewax.entity.OperationLog;
import com.beewax.entity.PaymentLog;
import com.beewax.entity.PaymentLog.AccountType;
import com.beewax.entity.User;
import com.beewax.exception.BusinessException;
import com.beewax.repository.AccountReceivableRepository;
import com.beewax.repository.CustomerRepository;
import com.beewax.repository.OperationLogRepository;
import com.beewax.repository.PaymentLogRepository;
import com.beewax.repository.ReceivableSummaryProjection;
import com.beewax.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

	private static final String ENTITY_TYPE_RECEIVABLE = "account_receivables";
	private static final String ACTION_PAYMENT_LOG = "PAYMENT_LOG";

	private final AccountReceivableRepository accountReceivableRepository;
	private final PaymentLogRepository paymentLogRepository;
	private final CustomerRepository customerRepository;
	private final OperationLogRepository operationLogRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public AccountService(
			AccountReceivableRepository accountReceivableRepository,
			PaymentLogRepository paymentLogRepository,
			CustomerRepository customerRepository,
			OperationLogRepository operationLogRepository,
			UserRepository userRepository,
			ObjectMapper objectMapper) {
		this.accountReceivableRepository = accountReceivableRepository;
		this.paymentLogRepository = paymentLogRepository;
		this.customerRepository = customerRepository;
		this.operationLogRepository = operationLogRepository;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
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

	@Transactional
	public ReceivablePaymentResponse registerReceivablePayment(Long id, ReceivablePaymentRequest request) {
		AccountReceivable record = accountReceivableRepository.findById(id)
				.orElseThrow(() -> new BusinessException(404, "应收账款记录不存在"));

		BigDecimal amount = request.getAmount();
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(400, "还款金额必须大于0");
		}

		BigDecimal remainingAmount = record.getRemainingAmount();
		if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(400, "该应收账款已结清");
		}

		if (amount.compareTo(remainingAmount) > 0) {
			String formattedRemaining = remainingAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
			throw new BusinessException(400, "还款金额不能超过剩余欠款 " + formattedRemaining + " 元");
		}

		User operator = userRepository.findById(getCurrentUser().userId())
				.orElseThrow(() -> new BusinessException(401, "未登录或 token 过期"));

		Map<String, Object> beforeSnapshot = buildReceivableSnapshot(record);

		BigDecimal newPaidAmount = record.getPaidAmount().add(amount);
		BigDecimal newRemainingAmount = remainingAmount.subtract(amount);
		ReceivableStatus newStatus = newRemainingAmount.compareTo(BigDecimal.ZERO) == 0
				? ReceivableStatus.PAID
				: ReceivableStatus.PARTIAL;

		record.setPaidAmount(newPaidAmount);
		record.setRemainingAmount(newRemainingAmount);
		record.setStatus(newStatus);
		accountReceivableRepository.save(record);

		PaymentLog paymentLog = new PaymentLog();
		paymentLog.setAccountType(AccountType.RECEIVABLE);
		paymentLog.setAccountId(record.getId());
		paymentLog.setAmount(amount);
		paymentLog.setPaymentDate(request.getPaymentDate());
		paymentLog.setRemark(trimToNull(request.getRemark()));
		paymentLog.setCreatedBy(operator.getId());
		paymentLogRepository.save(paymentLog);

		savePaymentOperationLog(operator, record.getId(), beforeSnapshot, buildReceivableSnapshot(record), paymentLog);

		return new ReceivablePaymentResponse(newRemainingAmount, newStatus.name());
	}

	private void savePaymentOperationLog(
			User operator,
			Long accountId,
			Map<String, Object> beforeSnapshot,
			Map<String, Object> afterSnapshot,
			PaymentLog paymentLog) {
		OperationLog log = new OperationLog();
		log.setOperatorId(operator.getId());
		log.setOperatorName(operator.getName());
		log.setAction(ACTION_PAYMENT_LOG);
		log.setEntityType(ENTITY_TYPE_RECEIVABLE);
		log.setEntityId(accountId);
		log.setBeforeValue(toJson(beforeSnapshot));
		log.setAfterValue(toJson(Map.of(
				"receivable", afterSnapshot,
				"paymentLog", Map.of(
						"id", paymentLog.getId(),
						"amount", paymentLog.getAmount(),
						"paymentDate", paymentLog.getPaymentDate().toString(),
						"remark", paymentLog.getRemark()))));
		operationLogRepository.save(log);
	}

	private Map<String, Object> buildReceivableSnapshot(AccountReceivable record) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("id", record.getId());
		snapshot.put("customerId", record.getCustomerId());
		snapshot.put("customerName", record.getCustomerName());
		snapshot.put("originalAmount", record.getOriginalAmount());
		snapshot.put("paidAmount", record.getPaidAmount());
		snapshot.put("remainingAmount", record.getRemainingAmount());
		snapshot.put("status", record.getStatus().name());
		return snapshot;
	}

	private JwtPrincipal getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
			throw new BusinessException(401, "未登录或 token 过期");
		}
		return principal;
	}

	private String toJson(Object data) {
		try {
			return objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException ex) {
			throw new BusinessException(500, "服务器内部错误");
		}
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
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
