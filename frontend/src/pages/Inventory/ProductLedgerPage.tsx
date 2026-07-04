import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
	getInventoryErrorMessage,
	getProductLedger,
	type LedgerEntry,
	type LedgerEntryType,
} from '../../api/inventory';
import Button from '../../components/Button/Button';
import DataTable from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import type { ActiveFilter, DateRangeValue } from '../../components/FilterToolbar/filterTypes';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import type { StatusBadgeStatus } from '../../components/StatusBadge/StatusBadge';
import { useAlertStore } from '../../store/alertStore';
import styles from './ProductLedgerPage.module.css';

const PAGE_SIZE = 20;
const DATE_FILTER_KEY = 'date';

function formatLedgerDate(dateStr: string): string {
	const [, month, day] = dateStr.split('-');
	return `${month}-${day}`;
}

function formatMoney(amount: number): string {
	return amount.toLocaleString('zh-CN', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});
}

function formatQty(qty: number, unit: string): string {
	return `${qty.toLocaleString('zh-CN', {
		minimumFractionDigits: 0,
		maximumFractionDigits: 3,
	})} ${unit}`;
}

function mapLedgerType(type: LedgerEntryType): StatusBadgeStatus {
	return type === 'INBOUND' ? '入库' : '出库';
}

function buildDateActiveFilters(range: DateRangeValue): ActiveFilter[] {
	const filters: ActiveFilter[] = [];
	if (range.start) {
		filters.push({ key: `${DATE_FILTER_KEY}:start`, label: '开始日期', value: range.start });
	}
	if (range.end) {
		filters.push({ key: `${DATE_FILTER_KEY}:end`, label: '结束日期', value: range.end });
	}
	return filters;
}

function getDateRangeFromActiveFilters(activeFilters: ActiveFilter[]): DateRangeValue {
	const start = activeFilters.find((filter) => filter.key === `${DATE_FILTER_KEY}:start`);
	const end = activeFilters.find((filter) => filter.key === `${DATE_FILTER_KEY}:end`);
	return {
		start: start?.value,
		end: end?.value,
	};
}

interface LedgerTableRow extends LedgerEntry {
	rowId: string;
}

export default function ProductLedgerPage() {
	const navigate = useNavigate();
	const { productId: productIdParam } = useParams();
	const productId = Number(productIdParam);
	const showAlert = useAlertStore((state) => state.showAlert);
	const [entries, setEntries] = useState<LedgerTableRow[]>([]);
	const [productName, setProductName] = useState('');
	const [unit, setUnit] = useState('');
	const [loading, setLoading] = useState(true);
	const [page, setPage] = useState(1);
	const [total, setTotal] = useState(0);
	const [activeFilters, setActiveFilters] = useState<ActiveFilter[]>([]);

	const dateRange = useMemo(
		() => getDateRangeFromActiveFilters(activeFilters),
		[activeFilters],
	);

	const loadLedger = useCallback(async () => {
		if (!Number.isFinite(productId) || productId <= 0) {
			showAlert('产品不存在');
			setLoading(false);
			return;
		}

		setLoading(true);
		try {
			const data = await getProductLedger(productId, {
				startDate: dateRange.start,
				endDate: dateRange.end,
				page,
				size: PAGE_SIZE,
			});
			setEntries(
				data.list.map((entry) => ({
					...entry,
					rowId: `${entry.type}-${entry.id}`,
				})),
			);
			setTotal(data.total);
			setProductName(data.productName);
			setUnit(data.unit);
		} catch (error) {
			showAlert(getInventoryErrorMessage(error));
		} finally {
			setLoading(false);
		}
	}, [productId, dateRange.start, dateRange.end, page, showAlert]);

	useEffect(() => {
		loadLedger();
	}, [loadLedger]);

	const handleFilterChange = (key: string, value: string | DateRangeValue) => {
		if (key !== DATE_FILTER_KEY || typeof value === 'string') {
			return;
		}

		setActiveFilters(buildDateActiveFilters(value));
		setPage(1);
	};

	const handleClearFilter = (key: string) => {
		const nextRange = getDateRangeFromActiveFilters(activeFilters);
		if (key === `${DATE_FILTER_KEY}:start`) {
			nextRange.start = undefined;
		}
		if (key === `${DATE_FILTER_KEY}:end`) {
			nextRange.end = undefined;
		}
		setActiveFilters(buildDateActiveFilters(nextRange));
		setPage(1);
	};

	const handleClearAll = () => {
		setActiveFilters([]);
		setPage(1);
	};

	const columns: ColumnsType<LedgerTableRow> = [
		{
			title: '日期',
			dataIndex: 'date',
			key: 'date',
			width: 100,
			render: (date: string) => formatLedgerDate(date),
		},
		{
			title: '类型',
			dataIndex: 'type',
			key: 'type',
			width: 90,
			render: (type: LedgerEntryType) => <StatusBadge status={mapLedgerType(type)} />,
		},
		{
			title: '数量',
			dataIndex: 'qty',
			key: 'qty',
			width: 120,
			align: 'right',
			render: (qty: number) => formatQty(qty, unit),
		},
		{
			title: '单价',
			dataIndex: 'unitPrice',
			key: 'unitPrice',
			width: 120,
			align: 'right',
			render: (price: number) => `¥ ${formatMoney(price)}`,
		},
		{
			title: '金额',
			dataIndex: 'amount',
			key: 'amount',
			width: 120,
			align: 'right',
			render: (amount: number) => `¥ ${formatMoney(amount)}`,
		},
		{
			title: '对手方',
			dataIndex: 'partyName',
			key: 'partyName',
		},
		{
			title: '备注',
			dataIndex: 'remark',
			key: 'remark',
			render: (remark: string | null) => remark ?? '—',
		},
	];

	return (
		<div>
			<div className={styles.header}>
				<Button variant="ghost" onClick={() => navigate('/inventory/stock')}>
					返回
				</Button>
				<h2 className={styles.productName}>{productName || '产品流水'}</h2>
				{unit ? <p className={styles.productMeta}>计量单位：{unit}</p> : null}
			</div>

			<div className={styles.toolbarWrap}>
				<FilterToolbar
					searchPlaceholder="搜索"
					onSearch={() => {}}
					filters={[{ key: DATE_FILTER_KEY, type: 'dateRange', label: '日期' }]}
					onFilterChange={handleFilterChange}
					activeFilters={activeFilters}
					onClearFilter={handleClearFilter}
					onClearAll={handleClearAll}
				/>
			</div>

			<DataTable<LedgerTableRow>
				columns={columns}
				dataSource={entries}
				loading={loading}
				rowKey="rowId"
				emptyText="暂无流水记录"
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
