package com.beewax.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ProductLedgerEntryProjection {

	Long getId();

	String getType();

	LocalDate getRecordDate();

	BigDecimal getQty();

	BigDecimal getUnitPrice();

	BigDecimal getAmount();

	String getPartyName();

	String getRemark();
}
