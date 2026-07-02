package com.beewax.repository;

import com.beewax.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);

	@Query(value = "SELECT COUNT(*) > 0 FROM account_payables "
			+ "WHERE supplier_id = :supplierId AND status IN ('UNPAID', 'PARTIAL')",
			nativeQuery = true)
	boolean hasUnpaidPayables(@Param("supplierId") Long supplierId);
}
