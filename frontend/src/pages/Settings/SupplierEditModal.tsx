import { Form, Input, Modal } from 'antd';
import { useEffect, useState } from 'react';
import {
	createSupplier,
	getSettingsErrorMessage,
	updateSupplier,
	type Supplier,
} from '../../api/settings';
import { useToast } from '../../hooks/useToast';

interface SupplierFormValues {
	name: string;
	contactName?: string;
	contactInfo?: string;
	remark?: string;
}

export interface SupplierEditModalProps {
	open: boolean;
	supplier: Supplier | null;
	onCancel: () => void;
	onSuccess: () => void;
}

export default function SupplierEditModal({
	open,
	supplier,
	onCancel,
	onSuccess,
}: SupplierEditModalProps) {
	const [form] = Form.useForm<SupplierFormValues>();
	const { showToast } = useToast();
	const [submitting, setSubmitting] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const isEdit = supplier !== null;

	useEffect(() => {
		if (!open) {
			return;
		}

		setError(null);
		if (supplier) {
			form.setFieldsValue({
				name: supplier.name,
				contactName: supplier.contactName ?? undefined,
				contactInfo: supplier.contactInfo ?? undefined,
				remark: supplier.remark ?? undefined,
			});
			return;
		}

		form.resetFields();
	}, [open, supplier, form]);

	const handleSubmit = async () => {
		try {
			const values = await form.validateFields();
			setSubmitting(true);
			setError(null);

			const payload = {
				name: values.name.trim(),
				contactName: values.contactName?.trim() || undefined,
				contactInfo: values.contactInfo?.trim() || undefined,
				remark: values.remark?.trim() || undefined,
			};

			if (isEdit && supplier) {
				await updateSupplier(supplier.id, payload);
			} else {
				await createSupplier(payload);
			}

			showToast('已保存', 'success');
			onSuccess();
			onCancel();
		} catch (submitError) {
			if (submitError && typeof submitError === 'object' && 'errorFields' in submitError) {
				return;
			}
			setError(getSettingsErrorMessage(submitError));
		} finally {
			setSubmitting(false);
		}
	};

	return (
		<Modal
			title={isEdit ? '编辑供应商' : '新增供应商'}
			open={open}
			onCancel={onCancel}
			onOk={handleSubmit}
			okText={isEdit ? '保存' : '新增'}
			cancelText="取消"
			confirmLoading={submitting}
			destroyOnHidden
		>
			{error ? (
				<p style={{ color: 'var(--color-brick)', marginBottom: 'var(--space-4)' }}>{error}</p>
			) : null}
			<Form<SupplierFormValues> form={form} layout="vertical">
				<Form.Item
					label="供应商名称"
					name="name"
					rules={[{ required: true, message: '请输入供应商名称' }]}
				>
					<Input placeholder="请输入供应商名称" />
				</Form.Item>
				<Form.Item label="联系人姓名" name="contactName">
					<Input placeholder="选填" />
				</Form.Item>
				<Form.Item label="联系方式" name="contactInfo">
					<Input placeholder="选填，电话或邮箱" />
				</Form.Item>
				<Form.Item label="备注" name="remark">
					<Input.TextArea placeholder="选填" rows={3} />
				</Form.Item>
			</Form>
		</Modal>
	);
}
