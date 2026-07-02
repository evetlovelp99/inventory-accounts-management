package com.beewax.service;

import com.beewax.config.JwtAuthenticationFilter.JwtPrincipal;
import com.beewax.dto.request.InboundCreateRequest;
import com.beewax.dto.response.InboundBatchResponse;
import com.beewax.dto.response.InboundCreateResponse;
import com.beewax.entity.InboundRecord;
import com.beewax.entity.OperationLog;
import com.beewax.entity.Product;
import com.beewax.entity.Product.ProductStatus;
import com.beewax.entity.Supplier;
import com.beewax.entity.User;
import com.beewax.exception.BusinessException;
import com.beewax.repository.InboundRecordRepository;
import com.beewax.repository.OperationLogRepository;
import com.beewax.repository.ProductRepository;
import com.beewax.repository.SupplierRepository;
import com.beewax.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

	private static final String ENTITY_TYPE_INBOUND = "inbound_records";
	private static final String ACTION_INBOUND_CREATE = "INBOUND_CREATE";

	private final InboundRecordRepository inboundRecordRepository;
	private final OperationLogRepository operationLogRepository;
	private final ProductRepository productRepository;
	private final SupplierRepository supplierRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public InventoryService(
			InboundRecordRepository inboundRecordRepository,
			OperationLogRepository operationLogRepository,
			ProductRepository productRepository,
			SupplierRepository supplierRepository,
			UserRepository userRepository,
			ObjectMapper objectMapper) {
		this.inboundRecordRepository = inboundRecordRepository;
		this.operationLogRepository = operationLogRepository;
		this.productRepository = productRepository;
		this.supplierRepository = supplierRepository;
		this.userRepository = userRepository;
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
		return snapshot;
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
}
