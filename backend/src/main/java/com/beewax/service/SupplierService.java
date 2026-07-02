package com.beewax.service;

import com.beewax.dto.request.SupplierCreateRequest;
import com.beewax.dto.request.SupplierStatusRequest;
import com.beewax.dto.request.SupplierUpdateRequest;
import com.beewax.dto.response.PageResponse;
import com.beewax.dto.response.SupplierResponse;
import com.beewax.entity.Supplier;
import com.beewax.entity.Supplier.SupplierStatus;
import com.beewax.exception.BusinessException;
import com.beewax.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

	private static final String DUPLICATE_NAME_MESSAGE = "已存在同名供应商";
	private static final String DEACTIVATE_WARNING_MESSAGE = "该供应商存在未付款记录，停用后仍可在账款模块查看，是否继续？";

	private final SupplierRepository supplierRepository;

	public SupplierService(SupplierRepository supplierRepository) {
		this.supplierRepository = supplierRepository;
	}

	public PageResponse<SupplierResponse> listSuppliers(String keyword, SupplierStatus status, int page, int size) {
		Specification<Supplier> spec = Specification.where(null);

		if (keyword != null && !keyword.isBlank()) {
			String pattern = "%" + keyword.trim() + "%";
			spec = spec.and((root, query, cb) -> cb.like(root.get("name"), pattern));
		}

		if (status != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
		}

		Page<Supplier> result = supplierRepository.findAll(spec, PageRequest.of(page - 1, size));
		List<SupplierResponse> list = result.getContent().stream()
				.map(SupplierResponse::from)
				.toList();

		return new PageResponse<>(list, result.getTotalElements(), page, size);
	}

	@Transactional
	public SupplierResponse createSupplier(SupplierCreateRequest request) {
		String name = request.getName().trim();
		if (supplierRepository.existsByName(name)) {
			throw new BusinessException(409, DUPLICATE_NAME_MESSAGE);
		}

		Supplier supplier = new Supplier();
		supplier.setName(name);
		supplier.setContactName(trimToNull(request.getContactName()));
		supplier.setContactInfo(trimToNull(request.getContactInfo()));
		supplier.setRemark(trimToNull(request.getRemark()));
		supplier.setStatus(SupplierStatus.ACTIVE);

		return SupplierResponse.from(supplierRepository.save(supplier));
	}

	@Transactional
	public SupplierResponse updateSupplier(Long id, SupplierUpdateRequest request) {
		Supplier supplier = findSupplierOrThrow(id);
		String name = request.getName().trim();

		if (supplierRepository.existsByNameAndIdNot(name, id)) {
			throw new BusinessException(409, DUPLICATE_NAME_MESSAGE);
		}

		supplier.setName(name);
		supplier.setContactName(trimToNull(request.getContactName()));
		supplier.setContactInfo(trimToNull(request.getContactInfo()));
		supplier.setRemark(trimToNull(request.getRemark()));

		return SupplierResponse.from(supplierRepository.save(supplier));
	}

	@Transactional
	public SupplierResponse updateSupplierStatus(Long id, SupplierStatusRequest request) {
		Supplier supplier = findSupplierOrThrow(id);

		if (request.getStatus() == SupplierStatus.INACTIVE) {
			boolean force = Boolean.TRUE.equals(request.getForce());
			if (supplierRepository.hasUnpaidPayables(id) && !force) {
				throw new BusinessException(400, DEACTIVATE_WARNING_MESSAGE);
			}
		}

		supplier.setStatus(request.getStatus());
		return SupplierResponse.from(supplierRepository.save(supplier));
	}

	private Supplier findSupplierOrThrow(Long id) {
		return supplierRepository.findById(id)
				.orElseThrow(() -> new BusinessException(404, "供应商不存在"));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
