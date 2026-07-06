package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReceivableDetailResponse {

	private String customerName;
	private List<ReceivableRecordResponse> records;
}
