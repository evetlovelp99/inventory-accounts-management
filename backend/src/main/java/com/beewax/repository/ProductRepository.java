package com.beewax.repository;

import com.beewax.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);

	@Query(value = "SELECT COALESCE(SUM(remaining_qty), 0) FROM inbound_records WHERE product_id = :productId",
			nativeQuery = true)
	BigDecimal sumRemainingQty(@Param("productId") Long productId);
}
