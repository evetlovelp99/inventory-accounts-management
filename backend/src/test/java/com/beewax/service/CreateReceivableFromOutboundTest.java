package com.beewax.service;

import com.beewax.entity.AccountReceivable;
import com.beewax.entity.OutboundRecord;
import com.beewax.entity.SettlementCurrency;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReceivableFromOutboundTest {

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
	void createReceivableFromOutbound_usd_copiesCurrencyFieldsFromOutbound() {
		OutboundRecord outbound = buildOutbound(
				SettlementCurrency.USD,
				new BigDecimal("7.1523"),
				new BigDecimal("1260.00"),
				new BigDecimal("9011.90"));
		User operator = buildOperator();

		when(accountReceivableRepository.save(any(AccountReceivable.class)))
				.thenAnswer(invocation -> {
					AccountReceivable saved = invocation.getArgument(0);
					saved.setId(99L);
					return saved;
				});

		Long receivableId = accountService.createReceivableFromOutbound(outbound, operator, "export order");

		ArgumentCaptor<AccountReceivable> captor = ArgumentCaptor.forClass(AccountReceivable.class);
		verify(accountReceivableRepository).save(captor.capture());
		AccountReceivable saved = captor.getValue();

		assertEquals(99L, receivableId);
		assertEquals(SettlementCurrency.USD, saved.getCurrency());
		assertEquals(new BigDecimal("7.1523"), saved.getExchangeRate());
		assertEquals(new BigDecimal("1260.00"), saved.getOriginalAmount());
		assertEquals(new BigDecimal("9011.90"), saved.getConvertedAmount());
		assertEquals(new BigDecimal("1260.00"), saved.getRemainingAmount());
		assertEquals(outbound.getId(), saved.getOutboundId());
		assertEquals("export order", saved.getRemark());
		verify(operationLogRepository).save(any());
	}

	@Test
	void createReceivableFromOutbound_cny_setsConvertedAmountEqualToOriginal() {
		OutboundRecord outbound = buildOutbound(
				SettlementCurrency.CNY,
				null,
				new BigDecimal("5000.00"),
				new BigDecimal("5000.00"));
		User operator = buildOperator();

		when(accountReceivableRepository.save(any(AccountReceivable.class)))
				.thenAnswer(invocation -> {
					AccountReceivable saved = invocation.getArgument(0);
					saved.setId(10L);
					return saved;
				});

		accountService.createReceivableFromOutbound(outbound, operator, null);

		ArgumentCaptor<AccountReceivable> captor = ArgumentCaptor.forClass(AccountReceivable.class);
		verify(accountReceivableRepository).save(captor.capture());
		AccountReceivable saved = captor.getValue();

		assertEquals(SettlementCurrency.CNY, saved.getCurrency());
		assertNull(saved.getExchangeRate());
		assertEquals(new BigDecimal("5000.00"), saved.getOriginalAmount());
		assertEquals(new BigDecimal("5000.00"), saved.getConvertedAmount());
	}

	private OutboundRecord buildOutbound(
			SettlementCurrency currency,
			BigDecimal exchangeRate,
			BigDecimal totalSaleAmount,
			BigDecimal convertedSaleAmount) {
		OutboundRecord outbound = new OutboundRecord();
		outbound.setId(22L);
		outbound.setCustomerId(3L);
		outbound.setCustomerName("上海贸易");
		outbound.setOutboundDate(LocalDate.of(2025, 6, 18));
		outbound.setCurrency(currency);
		outbound.setExchangeRate(exchangeRate);
		outbound.setTotalSaleAmount(totalSaleAmount);
		outbound.setConvertedSaleAmount(convertedSaleAmount);
		return outbound;
	}

	private User buildOperator() {
		User operator = new User();
		operator.setId(1L);
		operator.setName("仓管");
		return operator;
	}
}
