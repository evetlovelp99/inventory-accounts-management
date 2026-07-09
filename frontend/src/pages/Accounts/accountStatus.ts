import type { StatusBadgeStatus } from '../../components/StatusBadge/StatusBadge';
import type { PayableStatus, ReceivableStatus } from '../../api/accounts';

export type AccountStatus = ReceivableStatus | PayableStatus;

export function formatAccountStatus(status: AccountStatus): StatusBadgeStatus {
	switch (status) {
		case 'UNPAID':
			return '未结清';
		case 'PARTIAL':
			return '部分还款';
		case 'PAID':
			return '已结清';
	}
}
