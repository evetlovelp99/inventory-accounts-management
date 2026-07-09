export type ReceivableAgingTone = 'clay' | 'clayDark' | 'brick';

export function getReceivableAgingTone(days: number): ReceivableAgingTone {
	if (days > 30) {
		return 'brick';
	}
	if (days >= 15) {
		return 'clayDark';
	}
	return 'clay';
}

export function formatAgingDays(days: number): string {
	return `${days}天`;
}
