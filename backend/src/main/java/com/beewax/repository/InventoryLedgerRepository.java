package com.beewax.repository;

import com.beewax.entity.InboundRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface InventoryLedgerRepository extends Repository<InboundRecord, Long> {

	@Query(value = """
			SELECT ledger.id AS id,
			       ledger.type AS type,
			       ledger.recordDate AS recordDate,
			       ledger.qty AS qty,
			       ledger.unitPrice AS unitPrice,
			       ledger.amount AS amount,
			       ledger.partyName AS partyName,
			       ledger.remark AS remark
			FROM (
			  SELECT i.id AS id,
			         'INBOUND' AS type,
			         i.inbound_date AS recordDate,
			         i.quantity AS qty,
			         i.unit_price AS unitPrice,
			         i.total_amount AS amount,
			         i.supplier_name AS partyName,
			         i.remark AS remark
			  FROM inbound_records i
			  WHERE i.product_id = :productId
			    AND (:startDate IS NULL OR i.inbound_date >= :startDate)
			    AND (:endDate IS NULL OR i.inbound_date <= :endDate)
			  UNION ALL
			  SELECT o.id AS id,
			         'OUTBOUND' AS type,
			         o.outbound_date AS recordDate,
			         o.total_qty AS qty,
			         o.sale_unit_price AS unitPrice,
			         o.total_sale_amount AS amount,
			         o.customer_name AS partyName,
			         o.remark AS remark
			  FROM outbound_records o
			  WHERE o.product_id = :productId
			    AND (:startDate IS NULL OR o.outbound_date >= :startDate)
			    AND (:endDate IS NULL OR o.outbound_date <= :endDate)
			) ledger
			ORDER BY ledger.recordDate DESC, ledger.id DESC
			""",
			countQuery = """
			SELECT COUNT(*) FROM (
			  SELECT i.id
			  FROM inbound_records i
			  WHERE i.product_id = :productId
			    AND (:startDate IS NULL OR i.inbound_date >= :startDate)
			    AND (:endDate IS NULL OR i.inbound_date <= :endDate)
			  UNION ALL
			  SELECT o.id
			  FROM outbound_records o
			  WHERE o.product_id = :productId
			    AND (:startDate IS NULL OR o.outbound_date >= :startDate)
			    AND (:endDate IS NULL OR o.outbound_date <= :endDate)
			) ledger
			""",
			nativeQuery = true)
	Page<ProductLedgerEntryProjection> findProductLedger(
			@Param("productId") Long productId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			Pageable pageable);
}
