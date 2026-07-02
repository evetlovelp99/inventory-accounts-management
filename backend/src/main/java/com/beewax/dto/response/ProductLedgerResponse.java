package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProductLedgerResponse {

	private String productName;
	private String unit;
	private List<LedgerEntryResponse> list;
	private long total;
}
