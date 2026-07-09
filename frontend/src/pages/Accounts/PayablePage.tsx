import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
	getAccountsErrorMessage,
	getPayableDetail,
	listPayables,
	type PayableRecord,
	type PayableStatus,
	type PayableSummaryItem,
} from '../../api/accounts';
import Button from '../../components/Button/Button';
import DataTable from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import type { ActiveFilter, DateRangeValue } from '../../components/FilterToolbar/filterTypes';
import StatCard from '../../components/StatCard/StatCard';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import { useAlertStore } from '../../store/alertStore';
import { formatCurrencyAmount, formatMoney } from '../../utils/formatCurrencyAmount';
import { formatAccountStatus } from './accountStatus';
import PaymentModal from './PaymentModal';
import styles from './PayablePage.module.css';

const PAGE_SIZE = 20;
const STATUS_FILTER_KEY = 'status';

const STATUS_FILTER_OPTIONS = [
	{ value: 'UNPAID', label: '未结清' },
	{ value: 'PARTIAL', label: '部分还款' },
	{ value: 'PAID', label: '已结清' },
] as const;

function formatSummaryDate(dateStr: string): string {
	const [, month, day] = dateStr.split('-');
	return `${month}-${day}`;
}

function getStatusFilterLabel(value: string): string {
	return STATUS_FILTER_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

interface PaymentTarget {
	supplierId: number;
	supplierName: string;
	records: PayableRecord[];
}

export default function PayablePage() {
	const navigate = useNavigate();
	const showAlert = useAlertStore((state) => state.showAlert);
	const [items, setItems] = useState<PayableSummaryItem[]>([]);
	const [totalUnpaidAmount, setTotalUnpaidAmount] = useState(0);
	const [loading, setLoading] = useState(true);
	const [keyword, setKeyword] = useState('');
	const [page, setPage] = useState(1);
	const [total, setTotal] = useState(0);
	const [statusFilterValue, setStatusFilterValue] = useState<PayableStatus | undefined>();
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

	const loadPayables = useCallback(async () => {
		setLoading(true);
		try {
			const data = await listPayables({
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
		loadPayables();
	}, [loadPayables]);

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
			setStatusFilterValue(value as PayableStatus);
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

	const handleOpenPayment = async (row: PayableSummaryItem) => {
		setPaymentLoading(true);
		try {
			const detail = await getPayableDetail(row.supplierId);
			setPaymentTarget({
				supplierId: row.supplierId,
				supplierName: detail.supplierName,
				records: detail.records,
			});
		} catch (error) {
			showAlert(getAccountsErrorMessage(error));
		} finally {
			setPaymentLoading(false);
		}
	};

	const columns: ColumnsType<PayableSummaryItem> = [
		{
			title: '供应商',
			dataIndex: 'supplierName',
			key: 'supplierName',
		},
		{
			title: '原始金额',
			dataIndex: 'originalAmount',
			key: 'originalAmount',
			width: 140,
			align: 'right',
			render: (amount: number) => formatCurrencyAmount(amount),
		},
		{
			title: '已付金额',
			dataIndex: 'paidAmount',
			key: 'paidAmount',
			width: 140,
			align: 'right',
			render: (amount: number) => formatCurrencyAmount(amount),
		},
		{
			title: '剩余应付',
			dataIndex: 'remainingAmount',
			key: 'remainingAmount',
			width: 140,
			align: 'right',
			render: (amount: number) => formatCurrencyAmount(amount),
		},
		{
			title: '最早欠款日',
			dataIndex: 'oldestUnpaidDate',
			key: 'oldestUnpaidDate',
			width: 110,
			render: (date: string) => formatSummaryDate(date),
		},
		{
			title: '状态',
			dataIndex: 'status',
			key: 'status',
			width: 100,
			render: (status: PayableStatus) => (
				<StatusBadge status={formatAccountStatus(status)} />
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
							登记付款
						</Button>
					</div>
				),
		},
	];

	return (
		<div>
			<div className={styles.statGrid}>
				<StatCard
					label="未结清应付总额"
					value={`¥ ${formatMoney(totalUnpaidAmount)}`}
					loading={loading}
				/>
			</div>

			<div className={styles.toolbarRow}>
				<FilterToolbar
					searchPlaceholder="搜索供应商"
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

			<DataTable<PayableSummaryItem>
				columns={columns}
				dataSource={items}
				loading={loading}
				rowKey="supplierId"
				onRowClick={(row) =>
					navigate(`/accounts/payable/${row.supplierId}`, {
						state: { partyName: row.supplierName, currency: 'CNY' },
					})
				}
				emptyText={keyword ? '未找到匹配供应商' : '暂无未结清应付账款'}
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
				accountType="payable"
				customerName={paymentTarget?.supplierName ?? ''}
				currency="CNY"
				records={paymentTarget?.records ?? []}
				onCancel={() => setPaymentTarget(null)}
				onSuccess={loadPayables}
			/>
		</div>
	);
}
