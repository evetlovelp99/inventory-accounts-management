import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
	getAccountsErrorMessage,
	getPayableDetail,
	getReceivableDetail,
	type PaymentLogItem,
	type PayableRecord,
	type ReceivableRecord,
} from '../../api/accounts';
import type { SettlementCurrency } from '../../api/inventory';
import Button from '../../components/Button/Button';
import DataTable from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import type { ActiveFilter, DateRangeValue } from '../../components/FilterToolbar/filterTypes';
import StatusBadge from '../../components/StatusBadge/StatusBadge';
import { useAlertStore } from '../../store/alertStore';
import { formatCurrencyAmount } from '../../utils/formatCurrencyAmount';
import { formatAccountStatus } from './accountStatus';
import PaymentModal from './PaymentModal';
import styles from './AccountDetailPage.module.css';

const DATE_FILTER_KEY = 'date';

type AccountType = 'receivable' | 'payable';

interface AccountDetailLocationState {
	partyName?: string;
	currency?: SettlementCurrency;
}

interface AccountRecordRow {
	rowId: string;
	id: number;
	occurDate: string;
	originalAmount: number;
	paidAmount: number;
	remainingAmount: number;
	status: ReceivableRecord['status'];
	relatedId: number | null;
	relatedLabel: string;
	remark: string | null;
	paymentLogs: PaymentLogItem[];
}

function isAccountType(value: string | undefined): value is AccountType {
	return value === 'receivable' || value === 'payable';
}

function formatDetailDate(dateStr: string): string {
	return dateStr;
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

function sortPaymentLogs(logs: PaymentLogItem[]): PaymentLogItem[] {
	return [...logs].sort(
		(left, right) =>
			right.paymentDate.localeCompare(left.paymentDate) || right.id - left.id,
	);
}

function toReceivableRows(records: ReceivableRecord[]): AccountRecordRow[] {
	return records.map((record) => ({
		rowId: `receivable-${record.id}`,
		id: record.id,
		occurDate: record.occurDate,
		originalAmount: record.originalAmount,
		paidAmount: record.paidAmount,
		remainingAmount: record.remainingAmount,
		status: record.status,
		relatedId: record.outboundId,
		relatedLabel: '出库单',
		remark: record.remark,
		paymentLogs: sortPaymentLogs(record.paymentLogs),
	}));
}

function toPayableRows(records: PayableRecord[]): AccountRecordRow[] {
	return records.map((record) => ({
		rowId: `payable-${record.id}`,
		id: record.id,
		occurDate: record.occurDate,
		originalAmount: record.originalAmount,
		paidAmount: record.paidAmount,
		remainingAmount: record.remainingAmount,
		status: record.status,
		relatedId: record.inboundId,
		relatedLabel: '入库单',
		remark: record.remark,
		paymentLogs: sortPaymentLogs(record.paymentLogs),
	}));
}

interface PaymentLogsPanelProps {
	logs: PaymentLogItem[];
	currency: SettlementCurrency;
	emptyText: string;
}

function PaymentLogsPanel({ logs, currency, emptyText }: PaymentLogsPanelProps) {
	if (logs.length === 0) {
		return <p className={styles.paymentEmpty}>{emptyText}</p>;
	}

	return (
		<ul className={styles.paymentList}>
			{logs.map((log) => (
				<li key={log.id} className={styles.paymentItem}>
					<div className={styles.paymentDate}>{formatDetailDate(log.paymentDate)}</div>
					<div className={styles.paymentAmount}>
						{formatCurrencyAmount(log.amount, currency)}
					</div>
					<div className={styles.paymentRemark}>{log.remark ?? '—'}</div>
				</li>
			))}
		</ul>
	);
}

export default function AccountDetailPage() {
	const navigate = useNavigate();
	const { type, partyId: partyIdParam } = useParams();
	const location = useLocation();
	const locationState = (location.state as AccountDetailLocationState | null) ?? {};
	const showAlert = useAlertStore((state) => state.showAlert);

	const accountType = isAccountType(type) ? type : null;
	const partyId = Number(partyIdParam);

	const [partyName, setPartyName] = useState(locationState.partyName ?? '');
	const [rows, setRows] = useState<AccountRecordRow[]>([]);
	const [loading, setLoading] = useState(true);
	const [activeFilters, setActiveFilters] = useState<ActiveFilter[]>([]);
	const [paymentRecord, setPaymentRecord] = useState<ReceivableRecord | PayableRecord | null>(
		null,
	);

	const currency = locationState.currency ?? 'CNY';
	const isReceivable = accountType === 'receivable';
	const listPath = isReceivable ? '/accounts/receivable' : '/accounts/payable';
	const pageSubtitle = isReceivable ? '应收账款明细' : '应付账款明细';
	const paymentEmptyText = isReceivable ? '暂无还款记录' : '暂无付款记录';
	const paymentSectionTitle = isReceivable ? '还款流水' : '付款流水';

	const dateRange = useMemo(
		() => getDateRangeFromActiveFilters(activeFilters),
		[activeFilters],
	);

	const loadDetail = useCallback(async () => {
		if (!accountType || !Number.isFinite(partyId) || partyId <= 0) {
			showAlert('账款详情不存在');
			setLoading(false);
			return;
		}

		setLoading(true);
		try {
			if (accountType === 'receivable') {
				const detail = await getReceivableDetail(partyId, {
					startDate: dateRange.start,
					endDate: dateRange.end,
				});
				setPartyName(detail.customerName);
				setRows(toReceivableRows(detail.records));
				return;
			}

			const detail = await getPayableDetail(partyId, {
				startDate: dateRange.start,
				endDate: dateRange.end,
			});
			setPartyName(detail.supplierName);
			setRows(toPayableRows(detail.records));
		} catch (error) {
			showAlert(getAccountsErrorMessage(error));
		} finally {
			setLoading(false);
		}
	}, [accountType, partyId, dateRange.start, dateRange.end, showAlert]);

	useEffect(() => {
		loadDetail();
	}, [loadDetail]);

	const handleFilterChange = (key: string, value: string | DateRangeValue) => {
		if (key !== DATE_FILTER_KEY || typeof value === 'string') {
			return;
		}
		setActiveFilters(buildDateActiveFilters(value));
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
	};

	const handleClearAll = () => {
		setActiveFilters([]);
	};

	const paymentActionLabel = isReceivable ? '登记还款' : '登记付款';

	const handleOpenPayment = (row: AccountRecordRow) => {
		const baseRecord = {
			id: row.id,
			originalAmount: row.originalAmount,
			paidAmount: row.paidAmount,
			remainingAmount: row.remainingAmount,
			occurDate: row.occurDate,
			status: row.status,
			remark: row.remark,
			paymentLogs: row.paymentLogs,
		};

		if (isReceivable) {
			setPaymentRecord({
				...baseRecord,
				outboundId: row.relatedId,
			});
			return;
		}

		setPaymentRecord({
			...baseRecord,
			inboundId: row.relatedId,
		});
	};

	const columns: ColumnsType<AccountRecordRow> = [
		{
			title: '发生日期',
			dataIndex: 'occurDate',
			key: 'occurDate',
			width: 120,
			render: (date: string) => formatDetailDate(date),
		},
		{
			title: '原始金额',
			dataIndex: 'originalAmount',
			key: 'originalAmount',
			width: 130,
			align: 'right',
			render: (amount: number) => (
				<span className={styles.amountValue}>
					{formatCurrencyAmount(amount, currency)}
				</span>
			),
		},
		{
			title: isReceivable ? '已收金额' : '已付金额',
			dataIndex: 'paidAmount',
			key: 'paidAmount',
			width: 130,
			align: 'right',
			render: (amount: number) => (
				<span className={styles.amountValue}>
					{formatCurrencyAmount(amount, currency)}
				</span>
			),
		},
		{
			title: isReceivable ? '剩余欠款' : '剩余应付',
			dataIndex: 'remainingAmount',
			key: 'remainingAmount',
			width: 130,
			align: 'right',
			render: (amount: number) => (
				<span className={styles.amountValue}>
					{formatCurrencyAmount(amount, currency)}
				</span>
			),
		},
		{
			title: '状态',
			dataIndex: 'status',
			key: 'status',
			width: 100,
			render: (status: AccountRecordRow['status']) => (
				<StatusBadge status={formatAccountStatus(status)} />
			),
		},
		{
			title: '关联单号',
			key: 'relatedId',
			width: 110,
			render: (_, row) => (row.relatedId != null ? `${row.relatedLabel} #${row.relatedId}` : '—'),
		},
		{
			title: '备注',
			dataIndex: 'remark',
			key: 'remark',
			render: (remark: string | null) => remark ?? '—',
		},
	];

	columns.push({
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
					<Button variant="link" size="compact" onClick={() => handleOpenPayment(row)}>
						{paymentActionLabel}
					</Button>
				</div>
			),
	});

	if (!accountType || !Number.isFinite(partyId) || partyId <= 0) {
		return (
			<div>
				<Button variant="secondary" onClick={() => navigate('/accounts/receivable')}>
					返回
				</Button>
			</div>
		);
	}

	return (
		<div>
			<div className={styles.header}>
				<div className={styles.headerRow}>
					<div className={styles.headerMain}>
						<h2 className={styles.partyName}>{partyName || '账款详情'}</h2>
						<p className={styles.partyMeta}>{pageSubtitle}</p>
					</div>
					<Button variant="secondary" onClick={() => navigate(listPath)}>
						返回
					</Button>
				</div>
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

			<DataTable<AccountRecordRow>
				columns={columns}
				dataSource={rows}
				loading={loading}
				rowKey="rowId"
				emptyText="暂无账款明细"
				expandable={{
					expandedRowRender: (row) => (
						<div className={styles.paymentDetail}>
							<p className={styles.paymentTitle}>{paymentSectionTitle}</p>
							<PaymentLogsPanel
								logs={row.paymentLogs}
								currency={currency}
								emptyText={paymentEmptyText}
							/>
						</div>
					),
					rowExpandable: () => true,
				}}
			/>

			<PaymentModal
				open={paymentRecord !== null}
				accountType={accountType ?? 'receivable'}
				customerName={partyName}
				currency={currency}
				records={paymentRecord ? [paymentRecord] : []}
				onCancel={() => setPaymentRecord(null)}
				onSuccess={loadDetail}
			/>
		</div>
	);
}
