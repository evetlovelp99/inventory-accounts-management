import { Modal } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import {
	getSettingsErrorMessage,
	isSupplierDeactivateWarningError,
	listSuppliers,
	updateSupplierStatus,
	type Supplier,
} from '../../api/settings';
import Button from '../../components/Button/Button';
import DataTable from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import { useToast } from '../../hooks/useToast';
import { useAlertStore } from '../../store/alertStore';
import SupplierEditModal from './SupplierEditModal';
import styles from './ProductsPage.module.css';

const PAGE_SIZE = 20;

function formatSupplierStatus(status: Supplier['status']): string {
	return status === 'ACTIVE' ? '启用' : '停用';
}

export default function SuppliersPage() {
	const { showToast } = useToast();
	const showAlert = useAlertStore((state) => state.showAlert);
	const [suppliers, setSuppliers] = useState<Supplier[]>([]);
	const [loading, setLoading] = useState(true);
	const [keyword, setKeyword] = useState('');
	const [page, setPage] = useState(1);
	const [total, setTotal] = useState(0);
	const [modalOpen, setModalOpen] = useState(false);
	const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);

	const loadSuppliers = useCallback(async () => {
		setLoading(true);
		try {
			const data = await listSuppliers({
				keyword: keyword || undefined,
				page,
				size: PAGE_SIZE,
			});
			setSuppliers(data.list);
			setTotal(data.total);
		} catch (error) {
			showAlert(getSettingsErrorMessage(error));
		} finally {
			setLoading(false);
		}
	}, [keyword, page, showAlert]);

	useEffect(() => {
		loadSuppliers();
	}, [loadSuppliers]);

	const handleSearch = (value: string) => {
		setKeyword(value);
		setPage(1);
	};

	const handleCreate = () => {
		setEditingSupplier(null);
		setModalOpen(true);
	};

	const handleEdit = (supplier: Supplier) => {
		setEditingSupplier(supplier);
		setModalOpen(true);
	};

	const handleModalClose = () => {
		setModalOpen(false);
		setEditingSupplier(null);
	};

	const handleStatusChange = async (supplier: Supplier, nextStatus: Supplier['status']) => {
		const isDeactivate = nextStatus === 'INACTIVE';

		const performUpdate = async (force = false) => {
			try {
				await updateSupplierStatus(supplier.id, nextStatus, force);
				showToast(isDeactivate ? '供应商已停用' : '供应商已启用', 'success');
				await loadSuppliers();
			} catch (error) {
				if (isDeactivate && isSupplierDeactivateWarningError(error)) {
					Modal.confirm({
						title: '确认停用',
						content: getSettingsErrorMessage(error),
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
				content: `确定要停用供应商「${supplier.name}」吗？`,
				okText: '确认停用',
				cancelText: '取消',
				onOk: () => performUpdate(),
			});
			return;
		}

		await performUpdate();
	};

	const columns: ColumnsType<Supplier> = [
		{
			title: '供应商名称',
			dataIndex: 'name',
			key: 'name',
		},
		{
			title: '联系人',
			dataIndex: 'contactName',
			key: 'contactName',
			render: (contactName: string | null) => contactName ?? '—',
		},
		{
			title: '状态',
			dataIndex: 'status',
			key: 'status',
			width: 90,
			render: (status: Supplier['status']) => (
				<span
					className={status === 'ACTIVE' ? styles.statusActive : styles.statusInactive}
				>
					{formatSupplierStatus(status)}
				</span>
			),
		},
		{
			title: '操作',
			key: 'actions',
			width: 120,
			render: (_, record) => (
				<div className={styles.actions}>
					<Button variant="link" size="compact" onClick={() => handleEdit(record)}>
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
						searchPlaceholder="搜索供应商"
						onSearch={handleSearch}
						onFilterChange={() => {}}
						activeFilters={[]}
						onClearFilter={() => {}}
						onClearAll={() => {}}
					/>
				</div>
				<Button variant="primary" onClick={handleCreate}>
					新增供应商
				</Button>
			</div>

			<DataTable<Supplier>
				columns={columns}
				dataSource={suppliers}
				loading={loading}
				rowKey="id"
				emptyText="暂无供应商，请先新增"
				emptyAction={
					<Button variant="secondary" onClick={handleCreate}>
						新增供应商
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

			<SupplierEditModal
				open={modalOpen}
				supplier={editingSupplier}
				onCancel={handleModalClose}
				onSuccess={loadSuppliers}
			/>
		</div>
	);
}
