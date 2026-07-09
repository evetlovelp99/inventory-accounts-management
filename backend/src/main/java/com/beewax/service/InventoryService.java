package com.beewax.service;

import com.beewax.config.JwtAuthenticationFilter.JwtPrincipal;
import com.beewax.dto.request.InboundCreateRequest;
import com.beewax.dto.request.OutboundBatchLineRequest;
import com.beewax.dto.request.OutboundCreateRequest;
import com.beewax.dto.response.InboundBatchResponse;
import com.beewax.dto.response.InboundCreateResponse;
import com.beewax.dto.response.InboundProductionInfoResponse;
import com.beewax.dto.response.LedgerEntryResponse;
import com.beewax.dto.response.OutboundCreateResponse;
import com.beewax.dto.response.PageResponse;
import com.beewax.dto.response.ProductLedgerResponse;
import com.beewax.dto.response.StockOverviewResponse;
import com.beewax.entity.Customer;
import com.beewax.entity.Customer.CustomerStatus;
import com.beewax.entity.InboundRecord;
import com.beewax.entity.OperationLog;
import com.beewax.entity.OutboundBatchLine;
import com.beewax.entity.OutboundRecord;
import com.beewax.entity.Product;
import com.beewax.entity.SettlementCurrency;
import com.beewax.entity.Product.ProductStatus;
import com.beewax.entity.Supplier;
import com.beewax.entity.User;
import com.beewax.exception.BusinessException;
import com.beewax.repository.CustomerRepository;
import com.beewax.repository.InboundRecordRepository;
import com.beewax.repository.InventoryLedgerRepository;
import com.beewax.repository.OperationLogRepository;
import com.beewax.repository.OutboundBatchLineRepository;
import com.beewax.repository.OutboundRecordRepository;
import com.beewax.repository.ProductRepository;
import com.beewax.repository.ProductLedgerEntryProjection;
import com.beewax.repository.StockOverviewProjection;
import com.beewax.repository.SupplierRepository;
import com.beewax.repository.UserRepository;
import com.beewax.util.SettlementAmountCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.PessimisticLockingFailureException;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

	private static final String ENTITY_TYPE_INBOUND = "inbound_records";
	private static final String ENTITY_TYPE_OUTBOUND = "outbound_records";
	private static final String ACTION_INBOUND_CREATE = "INBOUND_CREATE";
	private static final String ACTION_OUTBOUND_CREATE = "OUTBOUND_CREATE";
	private static final String CONCURRENT_CONFLICT_MESSAGE = "库存余量已变化，请刷新批次列表后重试";

	private final InboundRecordRepository inboundRecordRepository;
	private final OutboundRecordRepository outboundRecordRepository;
	private final OutboundBatchLineRepository outboundBatchLineRepository;
	private final OperationLogRepository operationLogRepository;
	private final ProductRepository productRepository;
	private final InventoryLedgerRepository inventoryLedgerRepository;
	private final SupplierRepository supplierRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final AccountService accountService;
	private final ObjectMapper objectMapper;

	public InventoryService(
			InboundRecordRepository inboundRecordRepository,
			OutboundRecordRepository outboundRecordRepository,
			OutboundBatchLineRepository outboundBatchLineRepository,
			OperationLogRepository operationLogRepository,
			ProductRepository productRepository,
			InventoryLedgerRepository inventoryLedgerRepository,
			SupplierRepository supplierRepository,
			CustomerRepository customerRepository,
			UserRepository userRepository,
			AccountService accountService,
			ObjectMapper objectMapper) {
		this.inboundRecordRepository = inboundRecordRepository;
		this.outboundRecordRepository = outboundRecordRepository;
		this.outboundBatchLineRepository = outboundBatchLineRepository;
		this.operationLogRepository = operationLogRepository;
		this.productRepository = productRepository;
		this.inventoryLedgerRepository = inventoryLedgerRepository;
		this.supplierRepository = supplierRepository;
		this.customerRepository = customerRepository;
		this.userRepository = userRepository;
		this.accountService = accountService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public InboundCreateResponse createInbound(InboundCreateRequest request) {
		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new BusinessException(400, "产品不存在"));
		if (product.getStatus() == ProductStatus.INACTIVE) {
			throw new BusinessException(400, "产品已停用");
		}

		Supplier supplier = supplierRepository.findById(request.getSupplierId())
				.orElseThrow(() -> new BusinessException(400, "供应商不存在"));

		JwtPrincipal principal = getCurrentUser();
		User operator = userRepository.findById(principal.userId())
				.orElseThrow(() -> new BusinessException(401, "未登录或 token 过期"));

		BigDecimal quantity = request.getQuantity().setScale(3, RoundingMode.HALF_UP);
		BigDecimal unitPrice = request.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
		BigDecimal totalAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

		InboundRecord record = new InboundRecord();
		record.setProductId(product.getId());
		record.setSupplierId(supplier.getId());
		record.setSupplierName(supplier.getName());
		record.setInboundDate(request.getInboundDate());
		record.setQuantity(quantity);
		record.setUnit(product.getUnit());
		record.setUnitPrice(unitPrice);
		record.setTotalAmount(totalAmount);
		record.setRemainingQty(quantity);
		record.setRemark(trimToNull(request.getRemark()));
		record.setCreatedBy(operator.getId());
		record.setImported(false);
		record.setOriginPlace(trimToNull(request.getOriginPlace()));
		record.setHarvestDate(request.getHarvestDate());
		record.setInspectNo(trimToNull(request.getInspectNo()));
		record.setInspectOrg(trimToNull(request.getInspectOrg()));
		record.setInspectDate(request.getInspectDate());
		record.setInspectFileUrl(trimToNull(request.getInspectFileUrl()));
		record.setExpiryDate(request.getExpiryDate());

		InboundRecord saved = inboundRecordRepository.save(record);
		saveOperationLog(operator, saved);

		return new InboundCreateResponse(saved.getId(), totalAmount, null);
	}

	public List<InboundBatchResponse> listInboundBatches(Long productId) {
		if (!productRepository.existsById(productId)) {
			throw new BusinessException(404, "产品不存在");
		}

		return inboundRecordRepository
				.findByProductIdAndRemainingQtyGreaterThanOrderByInboundDateAscIdAsc(productId, BigDecimal.ZERO)
				.stream()
				.map(InboundBatchResponse::from)
				.toList();
	}

	public PageResponse<StockOverviewResponse> listStock(String keyword, int page, int size) {
		String searchKeyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
		Page<StockOverviewProjection> result = productRepository.findStockOverview(
				searchKeyword, PageRequest.of(page - 1, size));

		List<StockOverviewResponse> list = result.getContent().stream()
				.map(row -> new StockOverviewResponse(
						row.getProductId(),
						row.getProductName(),
						row.getSpec(),
						row.getUnit(),
						row.getTotalRemaining(),
						toLocalDate(row.getLastUpdated())))
				.toList();

		return new PageResponse<>(list, result.getTotalElements(), page, size);
	}

	public ProductLedgerResponse getProductLedger(
			Long productId, LocalDate startDate, LocalDate endDate, int page, int size) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new BusinessException(404, "产品不存在"));

		Page<ProductLedgerEntryProjection> result = inventoryLedgerRepository.findProductLedger(
				productId, startDate, endDate, PageRequest.of(page - 1, size));

		List<LedgerEntryResponse> list = result.getContent().stream()
				.map(row -> new LedgerEntryResponse(
						row.getId(),
						row.getType(),
						row.getRecordDate(),
						row.getQty(),
						row.getUnitPrice(),
						row.getAmount(),
						row.getCurrency(),
						row.getPartyName(),
						row.getRemark(),
						buildProductionInfo(row)))
				.toList();

		return new ProductLedgerResponse(product.getName(), product.getUnit(), list, result.getTotalElements());
	}

	@Transactional
	public OutboundCreateResponse createOutbound(OutboundCreateRequest request) {
		if (request.getBatchLines() == null || request.getBatchLines().isEmpty()) {
			throw new BusinessException(400, "请至少添加一行批次明细");
		}

		Product product = productRepository.findById(request.getProductId())
				.orElseThrow(() -> new BusinessException(400, "产品不存在"));
		if (product.getStatus() == ProductStatus.INACTIVE) {
			throw new BusinessException(400, "产品已停用");
		}

		Customer customer = customerRepository.findById(request.getCustomerId())
				.orElseThrow(() -> new BusinessException(400, "客户不存在"));
		if (customer.getStatus() == CustomerStatus.INACTIVE) {
			throw new BusinessException(400, "客户已停用");
		}

		JwtPrincipal principal = getCurrentUser();
		User operator = userRepository.findById(principal.userId())
				.orElseThrow(() -> new BusinessException(401, "未登录或 token 过期"));

		Set<Long> inboundIdSet = new HashSet<>();
		for (OutboundBatchLineRequest line : request.getBatchLines()) {
			if (!inboundIdSet.add(line.getInboundId())) {
				throw new BusinessException(400, "批次明细存在重复的入库批次");
			}
		}

		List<Long> inboundIds = inboundIdSet.stream().sorted().toList();
		List<InboundRecord> lockedRecords;
		try {
			lockedRecords = inboundRecordRepository.findAllByIdInForUpdate(inboundIds);
		} catch (PessimisticLockingFailureException ex) {
			throw new BusinessException(409, CONCURRENT_CONFLICT_MESSAGE);
		}

		if (lockedRecords.size() != inboundIds.size()) {
			throw new BusinessException(400, "入库批次不存在");
		}

		Map<Long, InboundRecord> recordById = lockedRecords.stream()
				.collect(Collectors.toMap(InboundRecord::getId, Function.identity()));

		BigDecimal saleUnitPrice = request.getSaleUnitPrice().setScale(2, RoundingMode.HALF_UP);
		BigDecimal totalQty = BigDecimal.ZERO;
		BigDecimal weightedCost = BigDecimal.ZERO;
		List<OutboundBatchLine> batchLines = new ArrayList<>();

		for (OutboundBatchLineRequest lineRequest : request.getBatchLines()) {
			InboundRecord inbound = recordById.get(lineRequest.getInboundId());
			if (!inbound.getProductId().equals(product.getId())) {
				throw new BusinessException(400, "入库批次与产品不匹配");
			}

			BigDecimal qty = lineRequest.getQty().setScale(3, RoundingMode.HALF_UP);
			if (qty.compareTo(inbound.getRemainingQty()) > 0) {
				throw new BusinessException(400,
						"批次 " + inbound.getId() + " 出库数量不能超过剩余余量 " + inbound.getRemainingQty());
			}

			inbound.setRemainingQty(inbound.getRemainingQty().subtract(qty));
			BigDecimal lineCost = qty.multiply(inbound.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);

			OutboundBatchLine batchLine = new OutboundBatchLine();
			batchLine.setInboundId(inbound.getId());
			batchLine.setQty(qty);
			batchLine.setUnitCost(inbound.getUnitPrice());
			batchLine.setLineCost(lineCost);
			batchLines.add(batchLine);

			totalQty = totalQty.add(qty);
			weightedCost = weightedCost.add(lineCost);
		}

		inboundRecordRepository.saveAll(lockedRecords);

		BigDecimal totalSaleAmount = totalQty.multiply(saleUnitPrice).setScale(2, RoundingMode.HALF_UP);
		SettlementCurrency currency = request.getCurrency() != null
				? request.getCurrency()
				: SettlementCurrency.CNY;
		BigDecimal exchangeRate = null;
		BigDecimal convertedSaleAmount;

		if (currency == SettlementCurrency.USD) {
			if (request.getExchangeRate() == null) {
				throw new BusinessException(400, "请填写汇率");
			}
			exchangeRate = request.getExchangeRate().setScale(4, RoundingMode.HALF_UP);
		}
		convertedSaleAmount = SettlementAmountCalculator.calculateConvertedAmount(
				currency, totalSaleAmount, exchangeRate);

		BigDecimal grossProfit = convertedSaleAmount.subtract(weightedCost).setScale(2, RoundingMode.HALF_UP);

		OutboundRecord outbound = new OutboundRecord();
		outbound.setProductId(product.getId());
		outbound.setCustomerId(customer.getId());
		outbound.setCustomerName(customer.getName());
		outbound.setOutboundDate(request.getOutboundDate());
		outbound.setTotalQty(totalQty);
		outbound.setUnit(product.getUnit());
		outbound.setSaleUnitPrice(saleUnitPrice);
		outbound.setTotalSaleAmount(totalSaleAmount);
		outbound.setCurrency(currency);
		outbound.setExchangeRate(exchangeRate);
		outbound.setConvertedSaleAmount(convertedSaleAmount);
		outbound.setWeightedCost(weightedCost);
		outbound.setGrossProfit(grossProfit);
		outbound.setRemark(trimToNull(request.getRemark()));
		outbound.setCreatedBy(operator.getId());
		outbound.setImported(false);

		OutboundRecord savedOutbound = outboundRecordRepository.save(outbound);
		for (OutboundBatchLine batchLine : batchLines) {
			batchLine.setOutboundId(savedOutbound.getId());
		}
		outboundBatchLineRepository.saveAll(batchLines);

		saveOutboundOperationLog(operator, savedOutbound, batchLines);

		Long receivableId = null;
		if (Boolean.TRUE.equals(request.getCreateReceivable())) {
			receivableId = accountService.createReceivableFromOutbound(
					savedOutbound, operator, request.getRemark());
		}

		return new OutboundCreateResponse(
				savedOutbound.getId(),
				totalQty,
				currency,
				totalSaleAmount,
				convertedSaleAmount,
				weightedCost,
				grossProfit,
				receivableId);
	}

	private void saveOperationLog(User operator, InboundRecord record) {
		OperationLog log = new OperationLog();
		log.setOperatorId(operator.getId());
		log.setOperatorName(operator.getName());
		log.setAction(ACTION_INBOUND_CREATE);
		log.setEntityType(ENTITY_TYPE_INBOUND);
		log.setEntityId(record.getId());
		log.setBeforeValue(null);
		log.setAfterValue(toJson(buildInboundSnapshot(record)));
		operationLogRepository.save(log);
	}

	private void saveOutboundOperationLog(User operator, OutboundRecord record, List<OutboundBatchLine> batchLines) {
		OperationLog log = new OperationLog();
		log.setOperatorId(operator.getId());
		log.setOperatorName(operator.getName());
		log.setAction(ACTION_OUTBOUND_CREATE);
		log.setEntityType(ENTITY_TYPE_OUTBOUND);
		log.setEntityId(record.getId());
		log.setBeforeValue(null);
		log.setAfterValue(toJson(buildOutboundSnapshot(record, batchLines)));
		operationLogRepository.save(log);
	}

	private Map<String, Object> buildOutboundSnapshot(OutboundRecord record, List<OutboundBatchLine> batchLines) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("id", record.getId());
		snapshot.put("productId", record.getProductId());
		snapshot.put("customerId", record.getCustomerId());
		snapshot.put("customerName", record.getCustomerName());
		snapshot.put("outboundDate", record.getOutboundDate().toString());
		snapshot.put("totalQty", record.getTotalQty());
		snapshot.put("unit", record.getUnit());
		snapshot.put("saleUnitPrice", record.getSaleUnitPrice());
		snapshot.put("totalSaleAmount", record.getTotalSaleAmount());
		snapshot.put("currency", record.getCurrency());
		snapshot.put("exchangeRate", record.getExchangeRate());
		snapshot.put("convertedSaleAmount", record.getConvertedSaleAmount());
		snapshot.put("weightedCost", record.getWeightedCost());
		snapshot.put("grossProfit", record.getGrossProfit());
		snapshot.put("remark", record.getRemark());
		snapshot.put("batchLines", batchLines.stream().map(line -> {
			Map<String, Object> lineSnapshot = new LinkedHashMap<>();
			lineSnapshot.put("inboundId", line.getInboundId());
			lineSnapshot.put("qty", line.getQty());
			lineSnapshot.put("unitCost", line.getUnitCost());
			lineSnapshot.put("lineCost", line.getLineCost());
			return lineSnapshot;
		}).toList());
		return snapshot;
	}

	private Map<String, Object> buildInboundSnapshot(InboundRecord record) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("id", record.getId());
		snapshot.put("productId", record.getProductId());
		snapshot.put("supplierId", record.getSupplierId());
		snapshot.put("supplierName", record.getSupplierName());
		snapshot.put("inboundDate", record.getInboundDate().toString());
		snapshot.put("quantity", record.getQuantity());
		snapshot.put("unit", record.getUnit());
		snapshot.put("unitPrice", record.getUnitPrice());
		snapshot.put("totalAmount", record.getTotalAmount());
		snapshot.put("remainingQty", record.getRemainingQty());
		snapshot.put("remark", record.getRemark());
		snapshot.put("originPlace", record.getOriginPlace());
		snapshot.put("harvestDate", record.getHarvestDate() != null ? record.getHarvestDate().toString() : null);
		snapshot.put("inspectNo", record.getInspectNo());
		snapshot.put("inspectOrg", record.getInspectOrg());
		snapshot.put("inspectDate", record.getInspectDate() != null ? record.getInspectDate().toString() : null);
		snapshot.put("inspectFileUrl", record.getInspectFileUrl());
		snapshot.put("expiryDate", record.getExpiryDate() != null ? record.getExpiryDate().toString() : null);
		return snapshot;
	}

	private InboundProductionInfoResponse buildProductionInfo(ProductLedgerEntryProjection row) {
		if (!"INBOUND".equals(row.getType())) {
			return null;
		}

		String originPlace = trimToNull(row.getOriginPlace());
		LocalDate harvestDate = row.getHarvestDate();
		String inspectNo = trimToNull(row.getInspectNo());
		String inspectOrg = trimToNull(row.getInspectOrg());
		LocalDate inspectDate = row.getInspectDate();
		String inspectFileUrl = trimToNull(row.getInspectFileUrl());
		LocalDate expiryDate = row.getExpiryDate();

		if (originPlace == null
				&& harvestDate == null
				&& inspectNo == null
				&& inspectOrg == null
				&& inspectDate == null
				&& inspectFileUrl == null
				&& expiryDate == null) {
			return null;
		}

		return new InboundProductionInfoResponse(
				originPlace,
				harvestDate,
				inspectNo,
				inspectOrg,
				inspectDate,
				inspectFileUrl,
				expiryDate);
	}

	private String toJson(Map<String, Object> data) {
		try {
			return objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException ex) {
			throw new BusinessException(500, "服务器内部错误");
		}
	}

	private JwtPrincipal getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
			throw new BusinessException(401, "未登录或 token 过期");
		}
		return principal;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private LocalDate toLocalDate(java.time.LocalDateTime dateTime) {
		return dateTime != null ? dateTime.toLocalDate() : null;
	}
}
