package com.beewax.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_payables")
@Getter
@Setter
public class AccountPayable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "supplier_id")
	private Long supplierId;

	@Column(name = "supplier_name", nullable = false, length = 100)
	private String supplierName;

	@Column(name = "inbound_id")
	private Long inboundId;

	@Column(name = "original_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal originalAmount;

	@Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal paidAmount;

	@Column(name = "remaining_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal remainingAmount;

	@Column(name = "occur_date", nullable = false)
	private LocalDate occurDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PayableStatus status;

	@Column(columnDefinition = "TEXT")
	private String remark;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Column(name = "is_imported", nullable = false)
	private Boolean imported = false;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	public enum PayableStatus {
		UNPAID, PARTIAL, PAID
	}
}
