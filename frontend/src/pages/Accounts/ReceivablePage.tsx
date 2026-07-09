import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import {
	getAccountsErrorMessage,
	listReceivables,
	type ReceivableStatus,
	type ReceivableSummaryItem,
} from '../../api/accounts';
import DataTable from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import type { StatusBadgeStatus } from '../../components/StatusBadge/StatusBadge';
import { useAlertStore } from '../../store/alertStore';
import { formatCurrencyAmount, formatMoney } from '../../utils/formatCurrencyAmount';
import styles from './ReceivablePage.module.css';

const PAGE_SIZE = 20;

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

export default function ReceivablePage() {
	const showAlert = useAlertStore((state) => state.showAlert);
	const [items, setItems] = useState<ReceivableSummaryItem[]>([]);
	const [totalUnpaidAmount, setTotalUnpaidAmount] = useState(0);
	const [loading, setLoading] = useState(true);
	const [keyword, setKeyword] = useState('');
	const [page, setPage] = useState(1);
	const [total, setTotal] = useState(0);

	const loadReceivables = useCallback(async () => {
		setLoading(true);
		try {
			const data = await listReceivables({
				keyword: keyword || undefined,
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
	}, [keyword, page, showAlert]);

	useEffect(() => {
		loadReceivables();
	}, [loadReceivables]);

	const handleSearch = (value: string) => {
		setKeyword(value);
		setPage(1);
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
			render: (amount: number, row) => formatCurrencyAmount(amount, row.currency),
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
			render: (days: number) => `${days} 天`,
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
	];

	return (
		<div>
			<div className={styles.summaryBar}>
				<p className={styles.summaryLabel}>未结清应收总额（折算人民币）</p>
				<p className={`${styles.summaryValue} tabular-nums`}>
					¥ {formatMoney(totalUnpaidAmount)}
				</p>
			</div>

			<div className={styles.toolbarRow}>
				<FilterToolbar
					searchPlaceholder="搜索客户"
					onSearch={handleSearch}
					onFilterChange={() => {}}
					activeFilters={[]}
					onClearFilter={() => {}}
					onClearAll={() => {}}
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
		</div>
	);
}
