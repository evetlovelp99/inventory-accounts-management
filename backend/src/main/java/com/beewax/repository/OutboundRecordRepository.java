package com.beewax.repository;

import com.beewax.entity.OutboundRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundRecordRepository extends JpaRepository<OutboundRecord, Long> {
}
