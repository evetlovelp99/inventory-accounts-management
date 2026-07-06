package com.beewax.repository;

import com.beewax.entity.PaymentLog;
import com.beewax.entity.PaymentLog.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

	List<PaymentLog> findByAccountTypeAndAccountIdInOrderByPaymentDateDescIdDesc(
			AccountType accountType, Collection<Long> accountIds);
}
