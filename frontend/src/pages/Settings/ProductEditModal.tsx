import { Form, Input, Modal } from 'antd';
import { useEffect, useState } from 'react';
import {
	createProduct,
	getSettingsErrorMessage,
	updateProduct,
	type Product,
} from '../../api/settings';
import { useToast } from '../../hooks/useToast';

interface ProductFormValues {
	name: string;
	spec?: string;
	unit: string;
}

export interface ProductEditModalProps {
	open: boolean;
	product: Product | null;
	onCancel: () => void;
	onSuccess: () => void;
}

export default function ProductEditModal({
	open,
	product,
	onCancel,
	onSuccess,
}: ProductEditModalProps) {
	const [form] = Form.useForm<ProductFormValues>();
	const { showToast } = useToast();
	const [submitting, setSubmitting] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const isEdit = product !== null;

	useEffect(() => {
		if (!open) {
			return;
		}

		setError(null);
		if (product) {
			form.setFieldsValue({
				name: product.name,
				spec: product.spec ?? undefined,
				unit: product.unit,
			});
			return;
		}

		form.resetFields();
	}, [open, product, form]);

	const handleSubmit = async () => {
		try {
			const values = await form.validateFields();
			setSubmitting(true);
			setError(null);

			const payload = {
				name: values.name.trim(),
				spec: values.spec?.trim() || undefined,
				unit: values.unit.trim(),
			};

			if (isEdit && product) {
				await updateProduct(product.id, payload);
			} else {
				await createProduct(payload);
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
			title={isEdit ? '编辑产品' : '新增产品'}
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
			<Form<ProductFormValues> form={form} layout="vertical">
				<Form.Item
					label="产品名称"
					name="name"
					rules={[{ required: true, message: '请输入产品名称' }]}
				>
					<Input placeholder="请输入产品名称" />
				</Form.Item>
				<Form.Item label="规格" name="spec">
					<Input placeholder="选填，如：一级" />
				</Form.Item>
				<Form.Item
					label="计量单位"
					name="unit"
					rules={[{ required: true, message: '请输入计量单位' }]}
				>
					<Input placeholder="如：kg、桶" />
				</Form.Item>
			</Form>
		</Modal>
	);
}
