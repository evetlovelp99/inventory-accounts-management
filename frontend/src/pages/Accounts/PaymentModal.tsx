import { Form, Input, Modal, Select } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import {
	getAccountsErrorMessage,
	registerPayablePayment,
	registerReceivablePayment,
	type PayableRecord,
	type ReceivableRecord,
} from '../../api/accounts';
import type { SettlementCurrency } from '../../api/inventory';
import Button from '../../components/Button/Button';
import { useToast } from '../../hooks/useToast';
import { formatCurrencyAmount } from '../../utils/formatCurrencyAmount';
import styles from './PaymentModal.module.css';

interface PaymentFormValues {
	amount: number;
	paymentDate: string;
	remark?: string;
}

type AccountPaymentType = 'receivable' | 'payable';
type PaymentRecord = ReceivableRecord | PayableRecord;

export interface PaymentModalProps {
	open: boolean;
	customerName: string;
	currency: SettlementCurrency;
	records: PaymentRecord[];
	accountType?: AccountPaymentType;
	onCancel: () => void;
	onSuccess: () => void;
}

function getTodayDate(): string {
	return new Date().toISOString().slice(0, 10);
}

function getOpenRecords(records: PaymentRecord[]): PaymentRecord[] {
	return records
		.filter(
			(record) => record.status !== 'PAID' && record.remainingAmount > 0,
		)
		.sort((left, right) => left.occurDate.localeCompare(right.occurDate));
}

function formatRecordLabel(record: PaymentRecord, currency: SettlementCurrency): string {
	const date = record.occurDate;
	const amount = formatCurrencyAmount(record.remainingAmount, currency);
	return `${date} · 剩余 ${amount}`;
}

export default function PaymentModal({
	open,
	customerName,
	currency,
	records,
	accountType = 'receivable',
	onCancel,
	onSuccess,
}: PaymentModalProps) {
	const isPayable = accountType === 'payable';
	const [form] = Form.useForm<PaymentFormValues>();
	const { showToast } = useToast();
	const [selectedRecordId, setSelectedRecordId] = useState<number | null>(null);
	const [submitting, setSubmitting] = useState(false);
	const [amountError, setAmountError] = useState<string | null>(null);

	const openRecords = useMemo(() => getOpenRecords(records), [records]);
	const selectedRecord = openRecords.find((record) => record.id === selectedRecordId) ?? null;

	useEffect(() => {
		if (!open) {
			return;
		}

		const defaultRecord = openRecords[0] ?? null;
		setSelectedRecordId(defaultRecord?.id ?? null);
		setAmountError(null);
		form.setFieldsValue({
			amount: undefined,
			paymentDate: getTodayDate(),
			remark: undefined,
		});
	}, [open, openRecords, form]);

	const validateAmount = (amount: number | undefined, remainingAmount: number): string | null => {
		const amountLabel = isPayable ? '付款' : '还款';
		if (amount == null || Number.isNaN(amount)) {
			return `请输入${amountLabel}金额`;
		}
		if (amount <= 0) {
			return `${amountLabel}金额必须大于0`;
		}
		if (amount > remainingAmount) {
			const remainingLabel = isPayable ? '剩余应付' : '剩余欠款';
			return `${amountLabel}金额不能超过${remainingLabel} ${formatCurrencyAmount(remainingAmount, currency)}`;
		}
		return null;
	};

	const handleSubmit = async () => {
		if (!selectedRecord) {
			return;
		}

		try {
			const values = await form.validateFields();
			const validationError = validateAmount(values.amount, selectedRecord.remainingAmount);
			if (validationError) {
				setAmountError(validationError);
				return;
			}

			setSubmitting(true);
			setAmountError(null);
			const payload = {
				amount: values.amount,
				paymentDate: values.paymentDate,
				remark: values.remark?.trim() || undefined,
			};
			if (isPayable) {
				await registerPayablePayment(selectedRecord.id, payload);
			} else {
				await registerReceivablePayment(selectedRecord.id, payload);
			}
			showToast(isPayable ? '付款已登记' : '还款已登记', 'success');
			onSuccess();
			onCancel();
		} catch (error) {
			if (error && typeof error === 'object' && 'errorFields' in error) {
				return;
			}
			setAmountError(getAccountsErrorMessage(error));
		} finally {
			setSubmitting(false);
		}
	};

	return (
		<Modal
			title={`${isPayable ? '登记付款' : '登记还款'} · ${customerName}`}
			open={open}
			onCancel={onCancel}
			footer={null}
			destroyOnHidden
			width={480}
		>
			{openRecords.length === 0 ? (
				<p className={styles.emptyHint}>
					{isPayable ? '该供应商暂无待付账款' : '该客户暂无待收账款'}
				</p>
			) : (
				<>
					{openRecords.length > 1 ? (
						<div className={styles.fieldBlock}>
							<label className={styles.fieldLabel} htmlFor="receivable-record-select">
								选择账款记录
							</label>
							<Select
								id="receivable-record-select"
								className={styles.recordSelect}
								value={selectedRecordId ?? undefined}
								options={openRecords.map((record) => ({
									value: record.id,
									label: formatRecordLabel(record, currency),
								}))}
								onChange={setSelectedRecordId}
							/>
						</div>
					) : null}

					{selectedRecord ? (
						<p className={styles.remainingHint}>
							{isPayable ? '当前剩余待付：' : '当前剩余待收：'}
							<strong className="tabular-nums">
								{formatCurrencyAmount(selectedRecord.remainingAmount, currency)}
							</strong>
						</p>
					) : null}

					<Form form={form} layout="vertical" onFinish={() => void handleSubmit()}>
						<Form.Item
							label={isPayable ? '付款金额' : '还款金额'}
							name="amount"
							rules={[
								{
									required: true,
									message: isPayable ? '请输入付款金额' : '请输入还款金额',
								},
							]}
							validateStatus={amountError ? 'error' : undefined}
							help={amountError ?? undefined}
						>
							<Input
								type="number"
								min={0}
								step="0.01"
								placeholder="0.00"
								onChange={() => setAmountError(null)}
							/>
						</Form.Item>

						<Form.Item
							label={isPayable ? '付款日期' : '还款日期'}
							name="paymentDate"
							rules={[
								{
									required: true,
									message: isPayable ? '请选择付款日期' : '请选择还款日期',
								},
							]}
						>
							<Input type="date" />
						</Form.Item>

						<Form.Item label="备注" name="remark">
							<Input.TextArea rows={2} placeholder="选填" />
						</Form.Item>

						<div className={styles.footer}>
							<Button variant="secondary" onClick={onCancel}>
								取消
							</Button>
							<Button
								variant="primary"
								loading={submitting}
								onClick={() => void handleSubmit()}
							>
								确认登记
							</Button>
						</div>
					</Form>
				</>
			)}
		</Modal>
	);
}
