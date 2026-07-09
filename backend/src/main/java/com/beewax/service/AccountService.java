package com.beewax.service;

import com.beewax.config.JwtAuthenticationFilter.JwtPrincipal;
import com.beewax.dto.request.PayableCreateRequest;
import com.beewax.dto.request.PayablePaymentRequest;
import com.beewax.dto.request.ReceivableCreateRequest;
import com.beewax.dto.request.ReceivablePaymentRequest;
import com.beewax.dto.response.PayableCreateResponse;
import com.beewax.dto.response.PayableDetailResponse;
import com.beewax.dto.response.PayableListResponse;
import com.beewax.dto.response.PayablePaymentResponse;
import com.beewax.dto.response.PayableRecordResponse;
import com.beewax.dto.response.PayableSummaryItemResponse;
import com.beewax.dto.response.PaymentLogResponse;
import com.beewax.dto.response.ReceivableCreateResponse;
import com.beewax.dto.response.ReceivableDetailResponse;
import com.beewax.dto.response.ReceivableListResponse;
import com.beewax.dto.response.ReceivablePaymentResponse;
import com.beewax.dto.response.ReceivableRecordResponse;
import com.beewax.dto.response.ReceivableSummaryItemResponse;
import com.beewax.entity.AccountPayable;
import com.beewax.entity.AccountPayable.PayableStatus;
import com.beewax.entity.AccountReceivable;
import com.beewax.entity.AccountReceivable.ReceivableStatus;
import com.beewax.entity.Customer;
import com.beewax.entity.Customer.CustomerStatus;
import com.beewax.entity.OperationLog;
import com.beewax.entity.OutboundRecord;
import com.beewax.entity.PaymentLog;
import com.beewax.entity.PaymentLog.AccountType;
import com.beewax.entity.SettlementCurrency;
import com.beewax.entity.Supplier;
import com.beewax.entity.Supplier.SupplierStatus;
import com.beewax.entity.User;
import com.beewax.exception.BusinessException;
import com.beewax.repository.AccountPayableRepository;
import com.beewax.repository.AccountReceivableRepository;
import com.beewax.repository.CustomerRepository;
import com.beewax.repository.InboundRecordRepository;
import com.beewax.repository.OperationLogRepository;
import com.beewax.repository.OutboundRecordRepository;
import com.beewax.repository.PayableSummaryProjection;
import com.beewax.repository.PaymentLogRepository;
import com.beewax.repository.ReceivableSummaryProjection;
import com.beewax.repository.SupplierRepository;
import com.beewax.repository.UserRepository;
import com.beewax.util.SettlementAmountCalculator;
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
	private static final String ENTITY_TYPE_PAYABLE = "account_payables";
	private static final String ACTION_PAYMENT_LOG = "PAYMENT_LOG";
	private static final String ACTION_RECEIVABLE_CREATE = "RECEIVABLE_CREATE";
	private static final String ACTION_PAYABLE_CREATE = "PAYABLE_CREATE";

	private final AccountReceivableRepository accountReceivableRepository;
	private final AccountPayableRepository accountPayableRepository;
	private final PaymentLogRepository paymentLogRepository;
	private final CustomerRepository customerRepository;
	private final SupplierRepository supplierRepository;
	private final OutboundRecordRepository outboundRecordRepository;
	private final InboundRecordRepository inboundRecordRepository;
	private final OperationLogRepository operationLogRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public AccountService(
			AccountReceivableRepository accountReceivableRepository,
			AccountPayableRepository accountPayableRepository,
			PaymentLogRepository paymentLogRepository,
			CustomerRepository customerRepository,
			SupplierRepository supplierRepository,
			OutboundRecordRepository outboundRecordRepository,
			InboundRecordRepository inboundRecordRepository,
			OperationLogRepository operationLogRepository,
			UserRepository userRepository,
			ObjectMapper objectMapper) {
		this.accountReceivableRepository = accountReceivableRepository;
		this.accountPayableRepository = accountPayableRepository;
		this.paymentLogRepository = paymentLogRepository;
		this.customerRepository = customerRepository;
		this.supplierRepository = supplierRepository;
		this.outboundRecordRepository = outboundRecordRepository;
		this.inboundRecordRepository = inboundRecordRepository;
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
	public ReceivableCreateResponse createReceivable(ReceivableCreateRequest request) {
		Customer customer = customerRepository.findById(request.getCustomerId())
				.orElseThrow(() -> new BusinessException(400, "客户不存在"));
		if (customer.getStatus() == CustomerStatus.INACTIVE) {
			throw new BusinessException(400, "客户已停用");
		}

		if (request.getOutboundId() != null
				&& !outboundRecordRepository.existsById(request.getOutboundId())) {
			throw new BusinessException(400, "出库单不存在");
		}

		User operator = userRepository.findById(getCurrentUser().userId())
				.orElseThrow(() -> new BusinessException(401, "未登录或 token 过期"));

		BigDecimal originalAmount = request.getOriginalAmount();
		SettlementCurrency currency = SettlementCurrency.CNY;
		BigDecimal convertedAmount = SettlementAmountCalculator.calculateConvertedAmount(
				currency, originalAmount, null);

		AccountReceivable saved = persistReceivable(
				customer.getId(),
				customer.getName(),
				request.getOutboundId(),
				originalAmount,
				currency,
				null,
				convertedAmount,
				request.getOccurDate(),
				trimToNull(request.getRemark()),
				operator);
		saveReceivableCreateOperationLog(operator, saved);

		return new ReceivableCreateResponse(saved.getId());
	}

	@Transactional
	public Long createReceivableFromOutbound(OutboundRecord outbound, User operator, String remark) {
		SettlementCurrency currency = outbound.getCurrency() != null
				? outbound.getCurrency()
				: SettlementCurrency.CNY;

		AccountReceivable saved = persistReceivable(
				outbound.getCustomerId(),
				outbound.getCustomerName(),
				outbound.getId(),
				outbound.getTotalSaleAmount(),
				currency,
				outbound.getExchangeRate(),
				outbound.getConvertedSaleAmount(),
				outbound.getOutboundDate(),
				trimToNull(remark),
				operator);
		saveReceivableCreateOperationLog(operator, saved);
		return saved.getId();
	}

	private AccountReceivable persistReceivable(
			Long customerId,
			String customerName,
			Long outboundId,
			BigDecimal originalAmount,
			SettlementCurrency currency,
			BigDecimal exchangeRate,
			BigDecimal convertedAmount,
			LocalDate occurDate,
			String remark,
			User operator) {
		AccountReceivable record = new AccountReceivable();
		record.setCustomerId(customerId);
		record.setCustomerName(customerName);
		record.setOutboundId(outboundId);
		record.setOriginalAmount(originalAmount);
		record.setCurrency(currency);
		record.setExchangeRate(exchangeRate);
		record.setConvertedAmount(convertedAmount);
		record.setPaidAmount(BigDecimal.ZERO);
		record.setRemainingAmount(originalAmount);
		record.setOccurDate(occurDate);
		record.setStatus(ReceivableStatus.UNPAID);
		record.setRemark(remark);
		record.setCreatedBy(operator.getId());
		record.setImported(false);
		return accountReceivableRepository.save(record);
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

	public PayableListResponse listPayables(String keyword, PayableStatus status, int page, int size) {
		String searchKeyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
		String statusFilter = status != null ? status.name() : null;

		BigDecimal totalUnpaidAmount = accountPayableRepository.sumRemainingAmount(searchKeyword, statusFilter);
		Page<PayableSummaryProjection> result = accountPayableRepository.findPayableSummary(
				searchKeyword, statusFilter, PageRequest.of(page - 1, size));

		List<PayableSummaryItemResponse> list = result.getContent().stream()
				.map(this::toPayableSummaryItem)
				.toList();

		return new PayableListResponse(totalUnpaidAmount, list, result.getTotalElements());
	}

	public PayableDetailResponse getPayableDetail(Long supplierId, LocalDate startDate, LocalDate endDate) {
		String supplierName = supplierRepository.findById(supplierId)
				.orElseThrow(() -> new BusinessException(404, "供应商不存在"))
				.getName();

		List<AccountPayable> records = accountPayableRepository.findBySupplierIdWithDateFilter(
				supplierId, startDate, endDate);
		Map<Long, List<PaymentLogResponse>> paymentLogsByAccountId = loadPaymentLogs(
				records.stream().map(AccountPayable::getId).toList(), AccountType.PAYABLE);

		List<PayableRecordResponse> recordResponses = records.stream()
				.map(record -> toPayableRecordResponse(
						record, paymentLogsByAccountId.getOrDefault(record.getId(), List.of())))
				.toList();

		return new PayableDetailResponse(supplierName, recordResponses);
	}

	@Transactional
	public PayableCreateResponse createPayable(PayableCreateRequest request) {
		Supplier supplier = supplierRepository.findById(request.getSupplierId())
				.orElseThrow(() -> new BusinessException(400, "供应商不存在"));
		if (supplier.getStatus() == SupplierStatus.INACTIVE) {
			throw new BusinessException(400, "供应商已停用");
		}

		if (request.getInboundId() != null
				&& !inboundRecordRepository.existsById(request.getInboundId())) {
			throw new BusinessException(400, "入库单不存在");
		}

		User operator = userRepository.findById(getCurrentUser().userId())
				.orElseThrow(() -> new BusinessException(401, "未登录或 token 过期"));

		BigDecimal originalAmount = request.getOriginalAmount();
		AccountPayable record = new AccountPayable();
		record.setSupplierId(supplier.getId());
		record.setSupplierName(supplier.getName());
		record.setInboundId(request.getInboundId());
		record.setOriginalAmount(originalAmount);
		record.setPaidAmount(BigDecimal.ZERO);
		record.setRemainingAmount(originalAmount);
		record.setOccurDate(request.getOccurDate());
		record.setStatus(PayableStatus.UNPAID);
		record.setRemark(trimToNull(request.getRemark()));
		record.setCreatedBy(operator.getId());
		record.setImported(false);

		AccountPayable saved = accountPayableRepository.save(record);
		savePayableCreateOperationLog(operator, saved);

		return new PayableCreateResponse(saved.getId());
	}

	@Transactional
	public PayablePaymentResponse registerPayablePayment(Long id, PayablePaymentRequest request) {
		AccountPayable record = accountPayableRepository.findById(id)
				.orElseThrow(() -> new BusinessException(404, "应付账款记录不存在"));

		BigDecimal amount = request.getAmount();
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(400, "付款金额必须大于0");
		}

		BigDecimal remainingAmount = record.getRemainingAmount();
		if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(400, "该应付账款已结清");
		}

		if (amount.compareTo(remainingAmount) > 0) {
			String formattedRemaining = remainingAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
			throw new BusinessException(400, "付款金额不能超过剩余欠款 " + formattedRemaining + " 元");
		}

		User operator = userRepository.findById(getCurrentUser().userId())
				.orElseThrow(() -> new BusinessException(401, "未登录或 token 过期"));

		Map<String, Object> beforeSnapshot = buildPayableSnapshot(record);

		BigDecimal newPaidAmount = record.getPaidAmount().add(amount);
		BigDecimal newRemainingAmount = remainingAmount.subtract(amount);
		PayableStatus newStatus = newRemainingAmount.compareTo(BigDecimal.ZERO) == 0
				? PayableStatus.PAID
				: PayableStatus.PARTIAL;

		record.setPaidAmount(newPaidAmount);
		record.setRemainingAmount(newRemainingAmount);
		record.setStatus(newStatus);
		accountPayableRepository.save(record);

		PaymentLog paymentLog = new PaymentLog();
		paymentLog.setAccountType(AccountType.PAYABLE);
		paymentLog.setAccountId(record.getId());
		paymentLog.setAmount(amount);
		paymentLog.setPaymentDate(request.getPaymentDate());
		paymentLog.setRemark(trimToNull(request.getRemark()));
		paymentLog.setCreatedBy(operator.getId());
		paymentLogRepository.save(paymentLog);

		savePayablePaymentOperationLog(operator, record.getId(), beforeSnapshot, buildPayableSnapshot(record), paymentLog);

		return new PayablePaymentResponse(newRemainingAmount, newStatus.name());
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
		log.setAfterValue(toJson(buildPaymentAfterSnapshot("receivable", afterSnapshot, paymentLog)));
		operationLogRepository.save(log);
	}

	private Map<String, Object> buildPaymentAfterSnapshot(
			String accountKey,
			Map<String, Object> afterSnapshot,
			PaymentLog paymentLog) {
		Map<String, Object> paymentLogSnapshot = new LinkedHashMap<>();
		paymentLogSnapshot.put("id", paymentLog.getId());
		paymentLogSnapshot.put("amount", paymentLog.getAmount());
		paymentLogSnapshot.put("paymentDate", paymentLog.getPaymentDate().toString());
		paymentLogSnapshot.put("remark", paymentLog.getRemark());

		Map<String, Object> result = new LinkedHashMap<>();
		result.put(accountKey, afterSnapshot);
		result.put("paymentLog", paymentLogSnapshot);
		return result;
	}

	private void savePayablePaymentOperationLog(
			User operator,
			Long accountId,
			Map<String, Object> beforeSnapshot,
			Map<String, Object> afterSnapshot,
			PaymentLog paymentLog) {
		OperationLog log = new OperationLog();
		log.setOperatorId(operator.getId());
		log.setOperatorName(operator.getName());
		log.setAction(ACTION_PAYMENT_LOG);
		log.setEntityType(ENTITY_TYPE_PAYABLE);
		log.setEntityId(accountId);
		log.setBeforeValue(toJson(beforeSnapshot));
		log.setAfterValue(toJson(buildPaymentAfterSnapshot("payable", afterSnapshot, paymentLog)));
		operationLogRepository.save(log);
	}

	private void savePayableCreateOperationLog(User operator, AccountPayable record) {
		OperationLog log = new OperationLog();
		log.setOperatorId(operator.getId());
		log.setOperatorName(operator.getName());
		log.setAction(ACTION_PAYABLE_CREATE);
		log.setEntityType(ENTITY_TYPE_PAYABLE);
		log.setEntityId(record.getId());
		log.setBeforeValue(null);
		log.setAfterValue(toJson(buildPayableSnapshot(record)));
		operationLogRepository.save(log);
	}

	private Map<String, Object> buildPayableSnapshot(AccountPayable record) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("id", record.getId());
		snapshot.put("supplierId", record.getSupplierId());
		snapshot.put("supplierName", record.getSupplierName());
		snapshot.put("originalAmount", record.getOriginalAmount());
		snapshot.put("paidAmount", record.getPaidAmount());
		snapshot.put("remainingAmount", record.getRemainingAmount());
		snapshot.put("occurDate", record.getOccurDate().toString());
		snapshot.put("inboundId", record.getInboundId());
		snapshot.put("remark", record.getRemark());
		snapshot.put("status", record.getStatus().name());
		return snapshot;
	}

	private void saveReceivableCreateOperationLog(User operator, AccountReceivable record) {
		OperationLog log = new OperationLog();
		log.setOperatorId(operator.getId());
		log.setOperatorName(operator.getName());
		log.setAction(ACTION_RECEIVABLE_CREATE);
		log.setEntityType(ENTITY_TYPE_RECEIVABLE);
		log.setEntityId(record.getId());
		log.setBeforeValue(null);
		log.setAfterValue(toJson(buildReceivableSnapshot(record)));
		operationLogRepository.save(log);
	}

	private Map<String, Object> buildReceivableSnapshot(AccountReceivable record) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("id", record.getId());
		snapshot.put("customerId", record.getCustomerId());
		snapshot.put("customerName", record.getCustomerName());
		snapshot.put("originalAmount", record.getOriginalAmount());
		snapshot.put("currency", record.getCurrency());
		snapshot.put("exchangeRate", record.getExchangeRate());
		snapshot.put("convertedAmount", record.getConvertedAmount());
		snapshot.put("paidAmount", record.getPaidAmount());
		snapshot.put("remainingAmount", record.getRemainingAmount());
		snapshot.put("occurDate", record.getOccurDate().toString());
		snapshot.put("outboundId", record.getOutboundId());
		snapshot.put("remark", record.getRemark());
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
		return loadPaymentLogs(records.stream().map(AccountReceivable::getId).toList(), AccountType.RECEIVABLE);
	}

	private Map<Long, List<PaymentLogResponse>> loadPaymentLogs(List<Long> accountIds, AccountType accountType) {
		if (accountIds.isEmpty()) {
			return Map.of();
		}

		List<PaymentLog> paymentLogs = paymentLogRepository
				.findByAccountTypeAndAccountIdInOrderByPaymentDateDescIdDesc(accountType, accountIds);

		Map<Long, List<PaymentLogResponse>> grouped = new LinkedHashMap<>();
		for (PaymentLog log : paymentLogs) {
			grouped.computeIfAbsent(log.getAccountId(), ignored -> new ArrayList<>())
					.add(toPaymentLogResponse(log));
		}
		return grouped;
	}

	private PayableSummaryItemResponse toPayableSummaryItem(PayableSummaryProjection row) {
		return new PayableSummaryItemResponse(
				row.getSupplierId(),
				row.getSupplierName(),
				row.getOriginalAmount(),
				row.getPaidAmount(),
				row.getRemainingAmount(),
				row.getOldestUnpaidDate(),
				row.getDaysSinceOldest() != null ? row.getDaysSinceOldest() : 0,
				row.getStatus());
	}

	private PayableRecordResponse toPayableRecordResponse(AccountPayable record, List<PaymentLogResponse> paymentLogs) {
		return new PayableRecordResponse(
				record.getId(),
				record.getOriginalAmount(),
				record.getPaidAmount(),
				record.getRemainingAmount(),
				record.getOccurDate(),
				record.getStatus().name(),
				record.getInboundId(),
				record.getRemark(),
				paymentLogs);
	}

	private ReceivableSummaryItemResponse toSummaryItem(ReceivableSummaryProjection row) {
		SettlementCurrency currency = row.getCurrency() != null
				? SettlementCurrency.valueOf(row.getCurrency())
				: SettlementCurrency.CNY;
		return new ReceivableSummaryItemResponse(
				row.getCustomerId(),
				row.getCustomerName(),
				currency,
				row.getOriginalAmount(),
				row.getConvertedAmount(),
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
