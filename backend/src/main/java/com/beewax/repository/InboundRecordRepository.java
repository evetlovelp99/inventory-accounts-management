package com.beewax.repository;

import com.beewax.entity.InboundRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface InboundRecordRepository extends JpaRepository<InboundRecord, Long> {

	List<InboundRecord> findByProductIdAndRemainingQtyGreaterThanOrderByInboundDateAscIdAsc(
			Long productId, BigDecimal remainingQty);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM InboundRecord i WHERE i.id IN :ids ORDER BY i.id ASC")
	List<InboundRecord> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);
}
