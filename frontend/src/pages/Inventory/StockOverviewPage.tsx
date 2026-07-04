import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
	getInventoryErrorMessage,
	listStock,
	type StockItem,
} from '../../api/inventory';
import Button from '../../components/Button/Button';
import DataTable, { DataTableRowClass } from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import { useAlertStore } from '../../store/alertStore';
import styles from '../Settings/ProductsPage.module.css';

const PAGE_SIZE = 20;

function formatLastUpdated(dateStr: string): string {
	const [, month, day] = dateStr.split('-');
	return `${month}-${day}`;
}

function formatRemainingQty(qty: number): string {
	return qty.toLocaleString('zh-CN', {
		minimumFractionDigits: 0,
		maximumFractionDigits: 3,
	});
}

export default function StockOverviewPage() {
	const navigate = useNavigate();
	const showAlert = useAlertStore((state) => state.showAlert);
	const [stockItems, setStockItems] = useState<StockItem[]>([]);
	const [loading, setLoading] = useState(true);
	const [keyword, setKeyword] = useState('');
	const [page, setPage] = useState(1);
	const [total, setTotal] = useState(0);

	const loadStock = useCallback(async () => {
		setLoading(true);
		try {
			const data = await listStock({
				keyword: keyword || undefined,
				page,
				size: PAGE_SIZE,
			});
			setStockItems(data.list);
			setTotal(data.total);
		} catch (error) {
			showAlert(getInventoryErrorMessage(error));
		} finally {
			setLoading(false);
		}
	}, [keyword, page, showAlert]);

	useEffect(() => {
		loadStock();
	}, [loadStock]);

	const handleSearch = (value: string) => {
		setKeyword(value);
		setPage(1);
	};

	const columns: ColumnsType<StockItem> = [
		{
			title: '产品名称',
			dataIndex: 'productName',
			key: 'productName',
		},
		{
			title: '规格',
			dataIndex: 'spec',
			key: 'spec',
			render: (spec: string | null) => spec ?? '—',
		},
		{
			title: '当前余量',
			dataIndex: 'totalRemaining',
			key: 'totalRemaining',
			width: 120,
			align: 'right',
			render: (qty: number) => formatRemainingQty(qty),
		},
		{
			title: '单位',
			dataIndex: 'unit',
			key: 'unit',
			width: 80,
		},
		{
			title: '最后更新',
			dataIndex: 'lastUpdated',
			key: 'lastUpdated',
			width: 100,
			render: (date: string) => formatLastUpdated(date),
		},
	];

	return (
		<div>
			<div className={styles.toolbarRow}>
				<div className={styles.toolbarMain}>
					<FilterToolbar
						searchPlaceholder="搜索产品"
						onSearch={handleSearch}
						onFilterChange={() => {}}
						activeFilters={[]}
						onClearFilter={() => {}}
						onClearAll={() => {}}
					/>
				</div>
			</div>

			<DataTable<StockItem>
				columns={columns}
				dataSource={stockItems}
				loading={loading}
				rowKey="productId"
				onRowClick={(row) => navigate(`/inventory/stock/${row.productId}`)}
				rowClassName={(row) =>
					row.totalRemaining === 0 ? DataTableRowClass.warning : ''
				}
				emptyText={
					keyword ? '未找到匹配产品，可新增产品' : '暂无库存，请先录入入库记录'
				}
				emptyAction={
					keyword ? undefined : (
						<Button variant="secondary" onClick={() => navigate('/inventory/inbound')}>
							录入入库
						</Button>
					)
				}
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
