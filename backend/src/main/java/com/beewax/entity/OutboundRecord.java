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
@Table(name = "outbound_records")
@Getter
@Setter
public class OutboundRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "customer_id")
	private Long customerId;

	@Column(name = "customer_name", length = 100)
	private String customerName;

	@Column(name = "outbound_date", nullable = false)
	private LocalDate outboundDate;

	@Column(name = "total_qty", nullable = false, precision = 15, scale = 3)
	private BigDecimal totalQty;

	@Column(nullable = false, length = 20)
	private String unit;

	@Column(name = "sale_unit_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal saleUnitPrice;

	@Column(name = "total_sale_amount", nullable = false, precision = 15, scale = 2)
	private BigDecimal totalSaleAmount;

	@Column(name = "weighted_cost", nullable = false, precision = 15, scale = 2)
	private BigDecimal weightedCost;

	@Column(name = "gross_profit", nullable = false, precision = 15, scale = 2)
	private BigDecimal grossProfit;

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
}
