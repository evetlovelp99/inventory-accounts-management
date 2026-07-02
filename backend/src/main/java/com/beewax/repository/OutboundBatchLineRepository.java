package com.beewax.repository;

import com.beewax.entity.OutboundBatchLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundBatchLineRepository extends JpaRepository<OutboundBatchLine, Long> {
}
