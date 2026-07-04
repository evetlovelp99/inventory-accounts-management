import { Form, Input, Modal } from 'antd';
import { useEffect, useState } from 'react';
import {
	createCustomer,
	getSettingsErrorMessage,
	updateCustomer,
	type Customer,
} from '../../api/settings';
import { useToast } from '../../hooks/useToast';

interface CustomerFormValues {
	name: string;
	country?: string;
	contactName?: string;
	contactInfo?: string;
	remark?: string;
}

export interface CustomerEditModalProps {
	open: boolean;
	customer: Customer | null;
	onCancel: () => void;
	onSuccess: () => void;
}

export default function CustomerEditModal({
	open,
	customer,
	onCancel,
	onSuccess,
}: CustomerEditModalProps) {
	const [form] = Form.useForm<CustomerFormValues>();
	const { showToast } = useToast();
	const [submitting, setSubmitting] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const isEdit = customer !== null;

	useEffect(() => {
		if (!open) {
			return;
		}

		setError(null);
		if (customer) {
			form.setFieldsValue({
				name: customer.name,
				country: customer.country ?? undefined,
				contactName: customer.contactName ?? undefined,
				contactInfo: customer.contactInfo ?? undefined,
				remark: customer.remark ?? undefined,
			});
			return;
		}

		form.resetFields();
	}, [open, customer, form]);

	const handleSubmit = async () => {
		try {
			const values = await form.validateFields();
			setSubmitting(true);
			setError(null);

			const payload = {
				name: values.name.trim(),
				country: values.country?.trim() || undefined,
				contactName: values.contactName?.trim() || undefined,
				contactInfo: values.contactInfo?.trim() || undefined,
				remark: values.remark?.trim() || undefined,
			};

			if (isEdit && customer) {
				await updateCustomer(customer.id, payload);
			} else {
				await createCustomer(payload);
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
			title={isEdit ? '编辑客户' : '新增客户'}
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
			<Form<CustomerFormValues> form={form} layout="vertical">
				<Form.Item
					label="客户名称"
					name="name"
					rules={[{ required: true, message: '请输入客户名称' }]}
				>
					<Input placeholder="请输入客户名称" />
				</Form.Item>
				<Form.Item label="国家/地区" name="country">
					<Input placeholder="选填" />
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
