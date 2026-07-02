package com.beewax.service;

import com.beewax.dto.request.ProductCreateRequest;
import com.beewax.dto.request.ProductStatusRequest;
import com.beewax.dto.request.ProductUpdateRequest;
import com.beewax.dto.response.PageResponse;
import com.beewax.dto.response.ProductResponse;
import com.beewax.entity.Product.ProductStatus;
import com.beewax.exception.BusinessException;
import com.beewax.repository.ProductRepository;
import com.beewax.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

	private static final String DUPLICATE_NAME_MESSAGE = "已存在同名产品，请修改名称";
	private static final String DEACTIVATE_WARNING_MESSAGE = "该产品仍有余量，停用后将不再显示在录入选项中";

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public PageResponse<ProductResponse> listProducts(String keyword, ProductStatus status, int page, int size) {
		Specification<Product> spec = Specification.where(null);

		if (keyword != null && !keyword.isBlank()) {
			String pattern = "%" + keyword.trim() + "%";
			spec = spec.and((root, query, cb) -> cb.like(root.get("name"), pattern));
		}

		if (status != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
		}

		Page<Product> result = productRepository.findAll(spec, PageRequest.of(page - 1, size));
		List<ProductResponse> list = result.getContent().stream()
				.map(ProductResponse::from)
				.toList();

		return new PageResponse<>(list, result.getTotalElements(), page, size);
	}

	@Transactional
	public ProductResponse createProduct(ProductCreateRequest request) {
		String name = request.getName().trim();
		if (productRepository.existsByName(name)) {
			throw new BusinessException(409, DUPLICATE_NAME_MESSAGE);
		}

		Product product = new Product();
		product.setName(name);
		product.setSpec(trimToNull(request.getSpec()));
		product.setUnit(request.getUnit().trim());
		product.setStatus(ProductStatus.ACTIVE);

		return ProductResponse.from(productRepository.save(product));
	}

	@Transactional
	public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
		Product product = findProductOrThrow(id);
		String name = request.getName().trim();

		if (productRepository.existsByNameAndIdNot(name, id)) {
			throw new BusinessException(409, DUPLICATE_NAME_MESSAGE);
		}

		product.setName(name);
		product.setSpec(trimToNull(request.getSpec()));
		product.setUnit(request.getUnit().trim());

		return ProductResponse.from(productRepository.save(product));
	}

	@Transactional
	public ProductResponse updateProductStatus(Long id, ProductStatusRequest request) {
		Product product = findProductOrThrow(id);

		if (request.getStatus() == ProductStatus.INACTIVE) {
			BigDecimal remainingQty = productRepository.sumRemainingQty(id);
			boolean force = Boolean.TRUE.equals(request.getForce());
			if (remainingQty.compareTo(BigDecimal.ZERO) > 0 && !force) {
				throw new BusinessException(400, DEACTIVATE_WARNING_MESSAGE);
			}
		}

		product.setStatus(request.getStatus());
		return ProductResponse.from(productRepository.save(product));
	}

	private Product findProductOrThrow(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new BusinessException(404, "产品不存在"));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
