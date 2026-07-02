package com.beewax.dto.response;

import com.beewax.entity.InboundRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class InboundBatchResponse {

	private Long inboundId;
	private LocalDate inboundDate;
	private BigDecimal unitPrice;
	private BigDecimal remainingQty;
	private String unit;
	private String supplierName;

	public static InboundBatchResponse from(InboundRecord record) {
		return new InboundBatchResponse(
				record.getId(),
				record.getInboundDate(),
				record.getUnitPrice(),
				record.getRemainingQty(),
				record.getUnit(),
				record.getSupplierName());
	}
}
