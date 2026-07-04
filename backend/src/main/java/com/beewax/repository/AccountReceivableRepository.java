package com.beewax.repository;

import com.beewax.entity.AccountReceivable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface AccountReceivableRepository extends JpaRepository<AccountReceivable, Long> {

	@Query(value = """
			SELECT
			  MAX(ar.customer_id) AS customerId,
			  ar.customer_name AS customerName,
			  SUM(ar.original_amount) AS originalAmount,
			  SUM(ar.paid_amount) AS paidAmount,
			  SUM(ar.remaining_amount) AS remainingAmount,
			  MIN(CASE WHEN ar.remaining_amount > 0 THEN ar.occur_date END) AS oldestUnpaidDate,
			  COALESCE(
			    DATEDIFF(CURDATE(), MIN(CASE WHEN ar.remaining_amount > 0 THEN ar.occur_date END)),
			    0
			  ) AS daysSinceOldest,
			  CASE
			    WHEN SUM(ar.remaining_amount) <= 0 THEN 'PAID'
			    WHEN SUM(ar.paid_amount) <= 0 THEN 'UNPAID'
			    ELSE 'PARTIAL'
			  END AS status
			FROM account_receivables ar
			WHERE (:keyword IS NULL OR :keyword = '' OR ar.customer_name LIKE CONCAT('%', :keyword, '%'))
			  AND (
			    (:status IS NULL AND ar.status IN ('UNPAID', 'PARTIAL'))
			    OR (:status IS NOT NULL AND ar.status = :status)
			  )
			GROUP BY COALESCE(ar.customer_id, 0), ar.customer_name
			ORDER BY daysSinceOldest DESC, ar.customer_name ASC
			""",
			countQuery = """
			SELECT COUNT(*) FROM (
			  SELECT 1
			  FROM account_receivables ar
			  WHERE (:keyword IS NULL OR :keyword = '' OR ar.customer_name LIKE CONCAT('%', :keyword, '%'))
			    AND (
			      (:status IS NULL AND ar.status IN ('UNPAID', 'PARTIAL'))
			      OR (:status IS NOT NULL AND ar.status = :status)
			    )
			  GROUP BY COALESCE(ar.customer_id, 0), ar.customer_name
			) grouped
			""",
			nativeQuery = true)
	Page<ReceivableSummaryProjection> findReceivableSummary(
			@Param("keyword") String keyword,
			@Param("status") String status,
			Pageable pageable);

	@Query(value = """
			SELECT COALESCE(SUM(ar.remaining_amount), 0)
			FROM account_receivables ar
			WHERE (:keyword IS NULL OR :keyword = '' OR ar.customer_name LIKE CONCAT('%', :keyword, '%'))
			  AND (
			    (:status IS NULL AND ar.status IN ('UNPAID', 'PARTIAL'))
			    OR (:status IS NOT NULL AND ar.status = :status)
			  )
			""",
			nativeQuery = true)
	BigDecimal sumRemainingAmount(
			@Param("keyword") String keyword,
			@Param("status") String status);
}
