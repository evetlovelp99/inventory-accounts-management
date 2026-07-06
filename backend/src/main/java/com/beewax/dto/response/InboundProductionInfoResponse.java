package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class InboundProductionInfoResponse {

	private String originPlace;
	private LocalDate harvestDate;
	private String inspectNo;
	private String inspectOrg;
	private LocalDate inspectDate;
	private String inspectFileUrl;
	private LocalDate expiryDate;
}
