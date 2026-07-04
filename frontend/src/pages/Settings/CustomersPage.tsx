import { Modal } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import {
	getSettingsErrorMessage,
	isCustomerDeactivateWarningError,
	listCustomers,
	updateCustomerStatus,
	type Customer,
} from '../../api/settings';
import Button from '../../components/Button/Button';
import DataTable from '../../components/DataTable/DataTable';
import FilterToolbar from '../../components/FilterToolbar/FilterToolbar';
import { useToast } from '../../hooks/useToast';
import { useAlertStore } from '../../store/alertStore';
import CustomerEditModal from './CustomerEditModal';
import styles from './ProductsPage.module.css';

const PAGE_SIZE = 20;

function formatCustomerStatus(status: Customer['status']): string {
	return status === 'ACTIVE' ? '启用' : '停用';
}

export default function CustomersPage() {
	const { showToast } = useToast();
	const showAlert = useAlertStore((state) => state.showAlert);
	const [customers, setCustomers] = useState<Customer[]>([]);
	const [loading, setLoading] = useState(true);
	const [keyword, setKeyword] = useState('');
	const [page, setPage] = useState(1);
	const [total, setTotal] = useState(0);
	const [modalOpen, setModalOpen] = useState(false);
	const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);

	const loadCustomers = useCallback(async () => {
		setLoading(true);
		try {
			const data = await listCustomers({
				keyword: keyword || undefined,
				page,
				size: PAGE_SIZE,
			});
			setCustomers(data.list);
			setTotal(data.total);
		} catch (error) {
			showAlert(getSettingsErrorMessage(error));
		} finally {
			setLoading(false);
		}
	}, [keyword, page, showAlert]);

	useEffect(() => {
		loadCustomers();
	}, [loadCustomers]);

	const handleSearch = (value: string) => {
		setKeyword(value);
		setPage(1);
	};

	const handleCreate = () => {
		setEditingCustomer(null);
		setModalOpen(true);
	};

	const handleEdit = (customer: Customer) => {
		setEditingCustomer(customer);
		setModalOpen(true);
	};

	const handleModalClose = () => {
		setModalOpen(false);
		setEditingCustomer(null);
	};

	const handleStatusChange = async (customer: Customer, nextStatus: Customer['status']) => {
		const isDeactivate = nextStatus === 'INACTIVE';

		const performUpdate = async (force = false) => {
			try {
				await updateCustomerStatus(customer.id, nextStatus, force);
				showToast(isDeactivate ? '客户已停用' : '客户已启用', 'success');
				await loadCustomers();
			} catch (error) {
				if (isDeactivate && isCustomerDeactivateWarningError(error)) {
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
				content: `确定要停用客户「${customer.name}」吗？`,
				okText: '确认停用',
				cancelText: '取消',
				onOk: () => performUpdate(),
			});
			return;
		}

		await performUpdate();
	};

	const columns: ColumnsType<Customer> = [
		{
			title: '客户名称',
			dataIndex: 'name',
			key: 'name',
		},
		{
			title: '国家/地区',
			dataIndex: 'country',
			key: 'country',
			render: (country: string | null) => country ?? '—',
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
			render: (status: Customer['status']) => (
				<span
					className={status === 'ACTIVE' ? styles.statusActive : styles.statusInactive}
				>
					{formatCustomerStatus(status)}
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
						searchPlaceholder="搜索客户"
						onSearch={handleSearch}
						onFilterChange={() => {}}
						activeFilters={[]}
						onClearFilter={() => {}}
						onClearAll={() => {}}
					/>
				</div>
				<Button variant="primary" onClick={handleCreate}>
					新增客户
				</Button>
			</div>

			<DataTable<Customer>
				columns={columns}
				dataSource={customers}
				loading={loading}
				rowKey="id"
				emptyText="暂无客户，请先新增"
				emptyAction={
					<Button variant="secondary" onClick={handleCreate}>
						新增客户
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

			<CustomerEditModal
				open={modalOpen}
				customer={editingCustomer}
				onCancel={handleModalClose}
				onSuccess={loadCustomers}
			/>
		</div>
	);
}
