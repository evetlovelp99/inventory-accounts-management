package com.beewax.repository;

import com.beewax.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);

	@Query(value = "SELECT COUNT(*) > 0 FROM account_receivables "
			+ "WHERE customer_id = :customerId AND status IN ('UNPAID', 'PARTIAL')",
			nativeQuery = true)
	boolean hasUnpaidReceivables(@Param("customerId") Long customerId);
}
