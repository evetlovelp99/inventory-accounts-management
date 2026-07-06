package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class PayableRecordResponse {

	private Long id;
	private BigDecimal originalAmount;
	private BigDecimal paidAmount;
	private BigDecimal remainingAmount;
	private LocalDate occurDate;
	private String status;
	private Long inboundId;
	private String remark;
	private List<PaymentLogResponse> paymentLogs;
}
