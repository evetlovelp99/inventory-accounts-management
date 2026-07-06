package com.beewax.repository;

import com.beewax.entity.AccountPayable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AccountPayableRepository extends JpaRepository<AccountPayable, Long> {

	@Query(value = """
			SELECT
			  MAX(ap.supplier_id) AS supplierId,
			  ap.supplier_name AS supplierName,
			  SUM(ap.original_amount) AS originalAmount,
			  SUM(ap.paid_amount) AS paidAmount,
			  SUM(ap.remaining_amount) AS remainingAmount,
			  MIN(CASE WHEN ap.remaining_amount > 0 THEN ap.occur_date END) AS oldestUnpaidDate,
			  COALESCE(
			    DATEDIFF(CURDATE(), MIN(CASE WHEN ap.remaining_amount > 0 THEN ap.occur_date END)),
			    0
			  ) AS daysSinceOldest,
			  CASE
			    WHEN SUM(ap.remaining_amount) <= 0 THEN 'PAID'
			    WHEN SUM(ap.paid_amount) <= 0 THEN 'UNPAID'
			    ELSE 'PARTIAL'
			  END AS status
			FROM account_payables ap
			WHERE (:keyword IS NULL OR :keyword = '' OR ap.supplier_name LIKE CONCAT('%', :keyword, '%'))
			  AND (
			    (:status IS NULL AND ap.status IN ('UNPAID', 'PARTIAL'))
			    OR (:status IS NOT NULL AND ap.status = :status)
			  )
			GROUP BY COALESCE(ap.supplier_id, 0), ap.supplier_name
			ORDER BY daysSinceOldest DESC, ap.supplier_name ASC
			""",
			countQuery = """
			SELECT COUNT(*) FROM (
			  SELECT 1
			  FROM account_payables ap
			  WHERE (:keyword IS NULL OR :keyword = '' OR ap.supplier_name LIKE CONCAT('%', :keyword, '%'))
			    AND (
			      (:status IS NULL AND ap.status IN ('UNPAID', 'PARTIAL'))
			      OR (:status IS NOT NULL AND ap.status = :status)
			    )
			  GROUP BY COALESCE(ap.supplier_id, 0), ap.supplier_name
			) grouped
			""",
			nativeQuery = true)
	Page<PayableSummaryProjection> findPayableSummary(
			@Param("keyword") String keyword,
			@Param("status") String status,
			Pageable pageable);

	@Query(value = """
			SELECT COALESCE(SUM(ap.remaining_amount), 0)
			FROM account_payables ap
			WHERE (:keyword IS NULL OR :keyword = '' OR ap.supplier_name LIKE CONCAT('%', :keyword, '%'))
			  AND (
			    (:status IS NULL AND ap.status IN ('UNPAID', 'PARTIAL'))
			    OR (:status IS NOT NULL AND ap.status = :status)
			  )
			""",
			nativeQuery = true)
	BigDecimal sumRemainingAmount(
			@Param("keyword") String keyword,
			@Param("status") String status);

	@Query("""
			SELECT ap FROM AccountPayable ap
			WHERE ap.supplierId = :supplierId
			  AND (:startDate IS NULL OR ap.occurDate >= :startDate)
			  AND (:endDate IS NULL OR ap.occurDate <= :endDate)
			ORDER BY ap.occurDate DESC, ap.id DESC
			""")
	List<AccountPayable> findBySupplierIdWithDateFilter(
			@Param("supplierId") Long supplierId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);
}
