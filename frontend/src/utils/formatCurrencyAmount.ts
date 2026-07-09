import type { SettlementCurrency } from '../api/inventory';

export function formatMoney(amount: number): string {
	return amount.toLocaleString('zh-CN', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});
}

export function formatCurrencyAmount(
	amount: number,
	currency: SettlementCurrency = 'CNY',
): string {
	const prefix = currency === 'USD' ? '$' : '¥';
	return `${prefix} ${formatMoney(amount)}`;
}

export function getLedgerEntryCurrency(
	type: 'INBOUND' | 'OUTBOUND',
	currency?: SettlementCurrency,
): SettlementCurrency {
	if (type === 'INBOUND') {
		return 'CNY';
	}
	return currency ?? 'CNY';
}
