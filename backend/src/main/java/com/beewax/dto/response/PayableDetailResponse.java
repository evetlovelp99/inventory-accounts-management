package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PayableDetailResponse {

	private String supplierName;
	private List<PayableRecordResponse> records;
}
