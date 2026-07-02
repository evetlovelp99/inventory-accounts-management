package com.beewax.repository;

import com.beewax.entity.InboundRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundRecordRepository extends JpaRepository<InboundRecord, Long> {
}
