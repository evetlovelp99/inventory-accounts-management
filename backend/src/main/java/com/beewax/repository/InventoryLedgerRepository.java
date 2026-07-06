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
			       ledger.remark AS remark,
			       ledger.originPlace AS originPlace,
			       ledger.harvestDate AS harvestDate,
			       ledger.inspectNo AS inspectNo,
			       ledger.inspectOrg AS inspectOrg,
			       ledger.inspectDate AS inspectDate,
			       ledger.inspectFileUrl AS inspectFileUrl,
			       ledger.expiryDate AS expiryDate
			FROM (
			  SELECT i.id AS id,
			         'INBOUND' AS type,
			         i.inbound_date AS recordDate,
			         i.quantity AS qty,
			         i.unit_price AS unitPrice,
			         i.total_amount AS amount,
			         i.supplier_name AS partyName,
			         i.remark AS remark,
			         i.origin_place AS originPlace,
			         i.harvest_date AS harvestDate,
			         i.inspect_no AS inspectNo,
			         i.inspect_org AS inspectOrg,
			         i.inspect_date AS inspectDate,
			         i.inspect_file_url AS inspectFileUrl,
			         i.expiry_date AS expiryDate
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
			         o.remark AS remark,
			         NULL AS originPlace,
			         NULL AS harvestDate,
			         NULL AS inspectNo,
			         NULL AS inspectOrg,
			         NULL AS inspectDate,
			         NULL AS inspectFileUrl,
			         NULL AS expiryDate
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
