package com.beewax.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "inbound_records")
@Getter
@Setter
public class InboundRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "supplier_id")
	private Long supplierId;

	@Column(name = "supplier_name", length = 100)
	private String supplierName;

	@Column(name = "inbound_date", nullable = false)
	private LocalDate inboundDate;

	@Column(nullable = false, precision = 15, scale = 3)
	private BigDecimal quantity;

	@Column(nullable = false, length = 20)
	private String unit;

	@Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "remaining_qty", nullable = false, precision = 15, scale = 3)
	private BigDecimal remainingQty;

	@Column(columnDefinition = "TEXT")
	private String remark;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Column(name = "is_imported", nullable = false)
	private Boolean imported = false;

	@Column(name = "origin_place", length = 100)
	private String originPlace;

	@Column(name = "harvest_date")
	private LocalDate harvestDate;

	@Column(name = "inspect_no", length = 50)
	private String inspectNo;

	@Column(name = "inspect_org", length = 100)
	private String inspectOrg;

	@Column(name = "inspect_date")
	private LocalDate inspectDate;

	@Column(name = "inspect_file_url", length = 255)
	private String inspectFileUrl;

	@Column(name = "expiry_date")
	private LocalDate expiryDate;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;
}
