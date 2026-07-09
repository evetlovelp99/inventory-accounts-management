package com.beewax.service;

import com.beewax.entity.AccountPayable;
import com.beewax.entity.InboundRecord;
import com.beewax.entity.User;
import com.beewax.repository.AccountPayableRepository;
import com.beewax.repository.AccountReceivableRepository;
import com.beewax.repository.CustomerRepository;
import com.beewax.repository.InboundRecordRepository;
import com.beewax.repository.OperationLogRepository;
import com.beewax.repository.OutboundRecordRepository;
import com.beewax.repository.PaymentLogRepository;
import com.beewax.repository.SupplierRepository;
import com.beewax.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePayableFromInboundTest {

	@Mock
	private AccountReceivableRepository accountReceivableRepository;
	@Mock
	private AccountPayableRepository accountPayableRepository;
	@Mock
	private PaymentLogRepository paymentLogRepository;
	@Mock
	private CustomerRepository customerRepository;
	@Mock
	private SupplierRepository supplierRepository;
	@Mock
	private OutboundRecordRepository outboundRecordRepository;
	@Mock
	private InboundRecordRepository inboundRecordRepository;
	@Mock
	private OperationLogRepository operationLogRepository;
	@Mock
	private UserRepository userRepository;

	private AccountService accountService;

	@BeforeEach
	void setUp() {
		accountService = new AccountService(
				accountReceivableRepository,
				accountPayableRepository,
				paymentLogRepository,
				customerRepository,
				supplierRepository,
				outboundRecordRepository,
				inboundRecordRepository,
				operationLogRepository,
				userRepository,
				new ObjectMapper());
	}

	@Test
	void createPayableFromInbound_copiesAmountAndLinksInboundRecord() {
		InboundRecord inbound = buildInbound();
		User operator = buildOperator();

		when(accountPayableRepository.save(any(AccountPayable.class)))
				.thenAnswer(invocation -> {
					AccountPayable saved = invocation.getArgument(0);
					saved.setId(88L);
					return saved;
				});

		Long payableId = accountService.createPayableFromInbound(inbound, operator, "unpaid batch");

		ArgumentCaptor<AccountPayable> captor = ArgumentCaptor.forClass(AccountPayable.class);
		verify(accountPayableRepository).save(captor.capture());
		AccountPayable saved = captor.getValue();

		assertEquals(88L, payableId);
		assertEquals(5L, saved.getSupplierId());
		assertEquals("华北供应商", saved.getSupplierName());
		assertEquals(12L, saved.getInboundId());
		assertEquals(new BigDecimal("12450.00"), saved.getOriginalAmount());
		assertEquals(new BigDecimal("12450.00"), saved.getRemainingAmount());
		assertEquals(BigDecimal.ZERO, saved.getPaidAmount());
		assertEquals(LocalDate.of(2025, 6, 10), saved.getOccurDate());
		assertEquals("unpaid batch", saved.getRemark());
		verify(operationLogRepository).save(any());
	}

	private InboundRecord buildInbound() {
		InboundRecord inbound = new InboundRecord();
		inbound.setId(12L);
		inbound.setSupplierId(5L);
		inbound.setSupplierName("华北供应商");
		inbound.setInboundDate(LocalDate.of(2025, 6, 10));
		inbound.setTotalAmount(new BigDecimal("12450.00"));
		return inbound;
	}

	private User buildOperator() {
		User operator = new User();
		operator.setId(2L);
		operator.setName("仓管");
		return operator;
	}
}
