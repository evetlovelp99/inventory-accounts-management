import { Form } from 'antd';
import { useState, type ReactNode } from 'react';
import Button from '../Button/Button';
import styles from './EntryForm.module.css';

export interface EntryFormProps {
	title: string;
	onSubmit: (values: Record<string, unknown>) => Promise<void>;
	onCancel: () => void;
	children: ReactNode;
	submitLabel?: string;
	loading?: boolean;
}

export default function EntryForm({
	title,
	onSubmit,
	onCancel,
	children,
	submitLabel = '提交',
	loading = false,
}: EntryFormProps) {
	const [form] = Form.useForm();
	const [submitting, setSubmitting] = useState(false);
	const isDisabled = submitting || loading;

	const handleFinish = async (values: Record<string, unknown>) => {
		setSubmitting(true);
		try {
			await onSubmit(values);
		} finally {
			setSubmitting(false);
		}
	};

	return (
		<section className={styles.card}>
			<h2 className={styles.title}>{title}</h2>
			<Form
				form={form}
				layout="vertical"
				className={styles.form}
				onFinish={handleFinish}
				disabled={isDisabled}
			>
				<div className={styles.fields}>{children}</div>
				<div className={styles.actions}>
					<Button variant="ghost" disabled={isDisabled} onClick={onCancel}>
						取消
					</Button>
					<Button
						variant="primary"
						disabled={isDisabled}
						onClick={() => form.submit()}
					>
						{submitting ? '提交中…' : submitLabel}
					</Button>
				</div>
			</Form>
		</section>
	);
}
