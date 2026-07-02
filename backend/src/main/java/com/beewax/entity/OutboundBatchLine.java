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
import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_batch_lines")
@Getter
@Setter
public class OutboundBatchLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "outbound_id", nullable = false)
	private Long outboundId;

	@Column(name = "inbound_id", nullable = false)
	private Long inboundId;

	@Column(nullable = false, precision = 15, scale = 3)
	private BigDecimal qty;

	@Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
	private BigDecimal unitCost;

	@Column(name = "line_cost", nullable = false, precision = 15, scale = 2)
	private BigDecimal lineCost;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;
}
