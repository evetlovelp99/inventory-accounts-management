package com.beewax.repository;

import com.beewax.entity.InboundRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface InboundRecordRepository extends JpaRepository<InboundRecord, Long> {

	List<InboundRecord> findByProductIdAndRemainingQtyGreaterThanOrderByInboundDateAscIdAsc(
			Long productId, BigDecimal remainingQty);
}
