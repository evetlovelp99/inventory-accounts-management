import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import {
	getAccountsErrorMessage,
	getReceivableDetail,
	listReceivables,
	type ReceivableRecord,
	type ReceivableStatus,
	type ReceivableSummaryItem,
} from '../../api/accounts';
import type { SettlementCurrency } from '../../api/inventory';
import Button from '../../components/Button/Button';
import DataTable from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import type { ActiveFilter, DateRangeValue } from '../../components/FilterToolbar/filterTypes';
import StatCard from '../../components/StatCard/StatCard';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import type { StatusBadgeStatus } from '../../components/StatusBadge/StatusBadge';
import { useAlertStore } from '../../store/alertStore';
import { formatCurrencyAmount, formatMoney } from '../../utils/formatCurrencyAmount';
import {
	formatAgingDays,
	getReceivableAgingTone,
	type ReceivableAgingTone,
} from '../../utils/receivableAging';
import PaymentModal from './PaymentModal';
import styles from './ReceivablePage.module.css';

const PAGE_SIZE = 20;
const STATUS_FILTER_KEY = 'status';

const STATUS_FILTER_OPTIONS = [
	{ value: 'UNPAID', label: '未结清' },
	{ value: 'PARTIAL', label: '部分还款' },
	{ value: 'PAID', label: '已结清' },
] as const;

function formatReceivableStatus(status: ReceivableStatus): StatusBadgeStatus {
	switch (status) {
		case 'UNPAID':
			return '未结清';
		case 'PARTIAL':
			return '部分还款';
		case 'PAID':
			return '已结清';
	}
}

function formatSummaryDate(dateStr: string): string {
	const [, month, day] = dateStr.split('-');
	return `${month}-${day}`;
}

function getAgingClassName(tone: ReceivableAgingTone): string {
	switch (tone) {
		case 'brick':
			return styles.agingBrick;
		case 'clayDark':
			return styles.agingClayDark;
		default:
			return styles.agingClay;
	}
}

function getStatusFilterLabel(value: string): string {
	return STATUS_FILTER_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

interface PaymentTarget {
	customerId: number;
	customerName: string;
	currency: SettlementCurrency;
	records: ReceivableRecord[];
}

export default function ReceivablePage() {
	const showAlert = useAlertStore((state) => state.showAlert);
	const [items, setItems] = useState<ReceivableSummaryItem[]>([]);
	const [totalUnpaidAmount, setTotalUnpaidAmount] = useState(0);
	const [loading, setLoading] = useState(true);
	const [keyword, setKeyword] = useState('');
	const [page, setPage] = useState(1);
	const [total, setTotal] = useState(0);
	const [statusFilterValue, setStatusFilterValue] = useState<ReceivableStatus | undefined>();
	const [paymentTarget, setPaymentTarget] = useState<PaymentTarget | null>(null);
	const [paymentLoading, setPaymentLoading] = useState(false);

	const activeFilters: ActiveFilter[] = statusFilterValue
		? [
				{
					key: STATUS_FILTER_KEY,
					label: '状态',
					value: statusFilterValue,
					displayValue: getStatusFilterLabel(statusFilterValue),
				},
			]
		: [];

	const loadReceivables = useCallback(async () => {
		setLoading(true);
		try {
			const data = await listReceivables({
				keyword: keyword || undefined,
				status: statusFilterValue,
				page,
				size: PAGE_SIZE,
			});
			setItems(data.list);
			setTotalUnpaidAmount(data.totalUnpaidAmount);
			setTotal(data.total);
		} catch (error) {
			showAlert(getAccountsErrorMessage(error));
		} finally {
			setLoading(false);
		}
	}, [keyword, statusFilterValue, page, showAlert]);

	useEffect(() => {
		loadReceivables();
	}, [loadReceivables]);

	const handleSearch = (value: string) => {
		setKeyword(value);
		setPage(1);
	};

	const handleFilterChange = (key: string, value: string | DateRangeValue) => {
		if (key !== STATUS_FILTER_KEY || typeof value !== 'string') {
			return;
		}

		if (!value) {
			setStatusFilterValue(undefined);
		} else {
			setStatusFilterValue(value as ReceivableStatus);
		}
		setPage(1);
	};

	const handleClearFilter = (key: string) => {
		if (key === STATUS_FILTER_KEY) {
			setStatusFilterValue(undefined);
			setPage(1);
		}
	};

	const handleClearAll = () => {
		setStatusFilterValue(undefined);
		setPage(1);
	};

	const handleOpenPayment = async (row: ReceivableSummaryItem) => {
		setPaymentLoading(true);
		try {
			const detail = await getReceivableDetail(row.customerId);
			setPaymentTarget({
				customerId: row.customerId,
				customerName: detail.customerName,
				currency: row.currency,
				records: detail.records,
			});
		} catch (error) {
			showAlert(getAccountsErrorMessage(error));
		} finally {
			setPaymentLoading(false);
		}
	};

	const columns: ColumnsType<ReceivableSummaryItem> = [
		{
			title: '客户',
			dataIndex: 'customerName',
			key: 'customerName',
		},
		{
			title: '原始金额',
			dataIndex: 'originalAmount',
			key: 'originalAmount',
			width: 140,
			align: 'right',
			render: (amount: number, row) => formatCurrencyAmount(amount, row.currency),
		},
		{
			title: '已收金额',
			dataIndex: 'paidAmount',
			key: 'paidAmount',
			width: 140,
			align: 'right',
			render: (amount: number, row) => formatCurrencyAmount(amount, row.currency),
		},
		{
			title: '剩余欠款',
			dataIndex: 'remainingAmount',
			key: 'remainingAmount',
			width: 140,
			align: 'right',
			render: (amount: number, row) => {
				const tone = getReceivableAgingTone(row.daysSinceOldest);
				return (
					<span className={getAgingClassName(tone)}>
						{formatCurrencyAmount(amount, row.currency)}
					</span>
				);
			},
		},
		{
			title: '最早欠款日',
			dataIndex: 'oldestUnpaidDate',
			key: 'oldestUnpaidDate',
			width: 110,
			render: (date: string) => formatSummaryDate(date),
		},
		{
			title: '账龄',
			dataIndex: 'daysSinceOldest',
			key: 'daysSinceOldest',
			width: 80,
			align: 'right',
			render: (days: number) => {
				const tone = getReceivableAgingTone(days);
				return (
					<span className={getAgingClassName(tone)}>{formatAgingDays(days)}</span>
				);
			},
		},
		{
			title: '状态',
			dataIndex: 'status',
			key: 'status',
			width: 100,
			render: (status: ReceivableStatus) => (
				<StatusBadge status={formatReceivableStatus(status)} />
			),
		},
		{
			title: '操作',
			key: 'actions',
			width: 110,
			render: (_, row) =>
				row.status === 'PAID' ? (
					'—'
				) : (
					<div
						className={styles.actions}
						onClick={(event) => event.stopPropagation()}
						onKeyDown={(event) => event.stopPropagation()}
					>
						<Button
							variant="link"
							size="compact"
							disabled={paymentLoading}
							onClick={() => void handleOpenPayment(row)}
						>
							登记还款
						</Button>
					</div>
				),
		},
	];

	return (
		<div>
			<div className={styles.statGrid}>
				<StatCard
					label="未结清应收总额（折算人民币）"
					value={`¥ ${formatMoney(totalUnpaidAmount)}`}
					loading={loading}
				/>
			</div>

			<div className={styles.toolbarRow}>
				<FilterToolbar
					searchPlaceholder="搜索客户"
					onSearch={handleSearch}
					filters={[
						{
							key: STATUS_FILTER_KEY,
							type: 'select',
							label: '状态',
							placeholder: '全部未结清',
							options: [...STATUS_FILTER_OPTIONS],
						},
					]}
					onFilterChange={handleFilterChange}
					activeFilters={activeFilters}
					onClearFilter={handleClearFilter}
					onClearAll={handleClearAll}
				/>
			</div>

			<DataTable<ReceivableSummaryItem>
				columns={columns}
				dataSource={items}
				loading={loading}
				rowKey="customerId"
				emptyText={keyword ? '未找到匹配客户' : '暂无未结清应收账款'}
				pagination={{
					current: page,
					pageSize: PAGE_SIZE,
					total,
					showSizeChanger: false,
					onChange: (nextPage) => setPage(nextPage),
				}}
			/>

			<PaymentModal
				open={paymentTarget !== null}
				customerName={paymentTarget?.customerName ?? ''}
				currency={paymentTarget?.currency ?? 'CNY'}
				records={paymentTarget?.records ?? []}
				onCancel={() => setPaymentTarget(null)}
				onSuccess={loadReceivables}
			/>
		</div>
	);
}
