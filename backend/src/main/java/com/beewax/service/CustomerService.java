package com.beewax.service;

import com.beewax.dto.request.CustomerCreateRequest;
import com.beewax.dto.request.CustomerStatusRequest;
import com.beewax.dto.request.CustomerUpdateRequest;
import com.beewax.dto.response.CustomerResponse;
import com.beewax.dto.response.PageResponse;
import com.beewax.entity.Customer;
import com.beewax.entity.Customer.CustomerStatus;
import com.beewax.exception.BusinessException;
import com.beewax.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

	private static final String DUPLICATE_NAME_MESSAGE = "已存在同名客户";
	private static final String DEACTIVATE_WARNING_MESSAGE = "该客户存在未收款记录，停用后仍可在账款模块查看，是否继续？";

	private final CustomerRepository customerRepository;

	public CustomerService(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	public PageResponse<CustomerResponse> listCustomers(String keyword, CustomerStatus status, int page, int size) {
		Specification<Customer> spec = Specification.where(null);

		if (keyword != null && !keyword.isBlank()) {
			String pattern = "%" + keyword.trim() + "%";
			spec = spec.and((root, query, cb) -> cb.like(root.get("name"), pattern));
		}

		if (status != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
		}

		Page<Customer> result = customerRepository.findAll(spec, PageRequest.of(page - 1, size));
		List<CustomerResponse> list = result.getContent().stream()
				.map(CustomerResponse::from)
				.toList();

		return new PageResponse<>(list, result.getTotalElements(), page, size);
	}

	@Transactional
	public CustomerResponse createCustomer(CustomerCreateRequest request) {
		String name = request.getName().trim();
		if (customerRepository.existsByName(name)) {
			throw new BusinessException(409, DUPLICATE_NAME_MESSAGE);
		}

		Customer customer = new Customer();
		customer.setName(name);
		customer.setCountry(trimToNull(request.getCountry()));
		customer.setContactName(trimToNull(request.getContactName()));
		customer.setContactInfo(trimToNull(request.getContactInfo()));
		customer.setRemark(trimToNull(request.getRemark()));
		customer.setStatus(CustomerStatus.ACTIVE);

		return CustomerResponse.from(customerRepository.save(customer));
	}

	@Transactional
	public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {
		Customer customer = findCustomerOrThrow(id);
		String name = request.getName().trim();

		if (customerRepository.existsByNameAndIdNot(name, id)) {
			throw new BusinessException(409, DUPLICATE_NAME_MESSAGE);
		}

		customer.setName(name);
		customer.setCountry(trimToNull(request.getCountry()));
		customer.setContactName(trimToNull(request.getContactName()));
		customer.setContactInfo(trimToNull(request.getContactInfo()));
		customer.setRemark(trimToNull(request.getRemark()));

		return CustomerResponse.from(customerRepository.save(customer));
	}

	@Transactional
	public CustomerResponse updateCustomerStatus(Long id, CustomerStatusRequest request) {
		Customer customer = findCustomerOrThrow(id);

		if (request.getStatus() == CustomerStatus.INACTIVE) {
			boolean force = Boolean.TRUE.equals(request.getForce());
			if (customerRepository.hasUnpaidReceivables(id) && !force) {
				throw new BusinessException(400, DEACTIVATE_WARNING_MESSAGE);
			}
		}

		customer.setStatus(request.getStatus());
		return CustomerResponse.from(customerRepository.save(customer));
	}

	private Customer findCustomerOrThrow(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new BusinessException(404, "客户不存在"));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
