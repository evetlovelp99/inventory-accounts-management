package com.beewax.repository;

import com.beewax.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	@Query(value = """
			SELECT p.id AS productId,
			       p.name AS productName,
			       p.spec AS spec,
			       p.unit AS unit,
			       COALESCE(SUM(i.remaining_qty), 0) AS totalRemaining,
			       MAX(i.updated_at) AS lastUpdated
			FROM products p
			LEFT JOIN inbound_records i ON i.product_id = p.id
			WHERE (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
			GROUP BY p.id, p.name, p.spec, p.unit
			ORDER BY p.id ASC
			""",
			countQuery = """
			SELECT COUNT(*) FROM products p
			WHERE (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
			""",
			nativeQuery = true)
	Page<StockOverviewProjection> findStockOverview(@Param("keyword") String keyword, Pageable pageable);
}
