import { Modal } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import {
	getSettingsErrorMessage,
	isDeactivateWarningError,
	listProducts,
	updateProductStatus,
	type Product,
} from '../../api/settings';
import Button from '../../components/Button/Button';
import DataTable from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import { useToast } from '../../hooks/useToast';
import { useAlertStore } from '../../store/alertStore';
import ProductEditModal from './ProductEditModal';
import styles from './ProductsPage.module.css';

const PAGE_SIZE = 20;

function formatProductStatus(status: Product['status']): string {
	return status === 'ACTIVE' ? '启用' : '停用';
}

export default function ProductsPage() {
	const { showToast } = useToast();
	const showAlert = useAlertStore((state) => state.showAlert);
	const [products, setProducts] = useState<Product[]>([]);
	const [loading, setLoading] = useState(true);
	const [keyword, setKeyword] = useState('');
	const [page, setPage] = useState(1);
	const [total, setTotal] = useState(0);
	const [modalOpen, setModalOpen] = useState(false);
	const [editingProduct, setEditingProduct] = useState<Product | null>(null);

	const loadProducts = useCallback(async () => {
		setLoading(true);
		try {
			const data = await listProducts({
				keyword: keyword || undefined,
				page,
				size: PAGE_SIZE,
			});
			setProducts(data.list);
			setTotal(data.total);
		} catch (error) {
			showAlert(getSettingsErrorMessage(error));
		} finally {
			setLoading(false);
		}
	}, [keyword, page, showAlert]);

	useEffect(() => {
		loadProducts();
	}, [loadProducts]);

	const handleSearch = (value: string) => {
		setKeyword(value);
		setPage(1);
	};

	const handleCreate = () => {
		setEditingProduct(null);
		setModalOpen(true);
	};

	const handleEdit = (product: Product) => {
		setEditingProduct(product);
		setModalOpen(true);
	};

	const handleModalClose = () => {
		setModalOpen(false);
		setEditingProduct(null);
	};

	const handleStatusChange = async (product: Product, nextStatus: Product['status']) => {
		const isDeactivate = nextStatus === 'INACTIVE';

		const performUpdate = async (force = false) => {
			try {
				await updateProductStatus(product.id, nextStatus, force);
				showToast(isDeactivate ? '产品已停用' : '产品已启用', 'success');
				await loadProducts();
			} catch (error) {
				if (isDeactivate && isDeactivateWarningError(error)) {
					Modal.confirm({
						title: '确认停用',
						content: `${getSettingsErrorMessage(error)}，是否继续？`,
						okText: '确认停用',
						cancelText: '取消',
						onOk: () => performUpdate(true),
					});
					return;
				}
				showAlert(getSettingsErrorMessage(error));
			}
		};

		if (isDeactivate) {
			Modal.confirm({
				title: '确认停用',
				content: `确定要停用产品「${product.name}」吗？`,
				okText: '确认停用',
				cancelText: '取消',
				onOk: () => performUpdate(),
			});
			return;
		}

		await performUpdate();
	};

	const columns: ColumnsType<Product> = [
		{
			title: '产品名称',
			dataIndex: 'name',
			key: 'name',
		},
		{
			title: '规格',
			dataIndex: 'spec',
			key: 'spec',
			render: (spec: string | null) => spec ?? '—',
		},
		{
			title: '计量单位',
			dataIndex: 'unit',
			key: 'unit',
			width: 100,
		},
		{
			title: '状态',
			dataIndex: 'status',
			key: 'status',
			width: 90,
			render: (status: Product['status']) => (
				<span
					className={status === 'ACTIVE' ? styles.statusActive : styles.statusInactive}
				>
					{formatProductStatus(status)}
				</span>
			),
		},
		{
			title: '操作',
			key: 'actions',
			width: 120,
			render: (_, record) => (
				<div className={styles.actions}>
					<Button
						variant="link"
						size="compact"
						onClick={() => handleEdit(record)}
					>
						编辑
					</Button>
					{record.status === 'ACTIVE' ? (
						<Button
							variant="link"
							size="compact"
							onClick={() => handleStatusChange(record, 'INACTIVE')}
						>
							停用
						</Button>
					) : (
						<Button
							variant="link"
							size="compact"
							onClick={() => handleStatusChange(record, 'ACTIVE')}
						>
							启用
						</Button>
					)}
				</div>
			),
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
				<Button variant="primary" onClick={handleCreate}>
					新增产品
				</Button>
			</div>

			<DataTable<Product>
				columns={columns}
				dataSource={products}
				loading={loading}
				rowKey="id"
				emptyText="暂无产品，请先新增"
				emptyAction={
					<Button variant="secondary" onClick={handleCreate}>
						新增产品
					</Button>
				}
				pagination={{
					current: page,
					pageSize: PAGE_SIZE,
					total,
					showSizeChanger: false,
					onChange: (nextPage) => setPage(nextPage),
				}}
			/>

			<ProductEditModal
				open={modalOpen}
				product={editingProduct}
				onCancel={handleModalClose}
				onSuccess={loadProducts}
			/>
		</div>
	);
}
