import { Checkbox, DatePicker, Form, Input, InputNumber, Radio, Select } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
	createOutbound,
	getCnyUsdExchangeRate,
	getInventoryErrorMessage,
	listInboundBatches,
	listStock,
	type SettlementCurrency,
	type StockItem,
} from '../../api/inventory';
import { listCustomers, type Customer } from '../../api/settings';
import BatchRow from '../../components/EntryForm/BatchRow';
import type { BatchRowValue, InboundBatch } from '../../components/EntryForm/batchTypes';
import ExchangeRateInput from '../../components/EntryForm/ExchangeRateInput';
import Button from '../../components/Button/Button';
import EntryForm from '../../components/EntryForm/EntryForm';
import { useToast } from '../../hooks/useToast';
import { useAlertStore } from '../../store/alertStore';
import styles from './OutboundEntryPage.module.css';

interface OutboundFormValues {
	productId: number;
	customerId: number;
	outboundDate: Dayjs;
	currency: SettlementCurrency;
	exchangeRate?: number | null;
	saleUnitPrice: number;
	createReceivable: boolean;
	remark?: string;
}

const DROPDOWN_PAGE_SIZE = 500;
const EMPTY_BATCH_LINE: BatchRowValue = { batchId: 0, qty: 0 };

function formatMoney(amount: number): string {
	return amount.toLocaleString('zh-CN', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});
}

function formatQty(qty: number): string {
	return qty.toLocaleString('zh-CN', {
		minimumFractionDigits: 0,
		maximumFractionDigits: 3,
	});
}

function formatStockLabel(item: StockItem): string {
	const spec = item.spec ? `（${item.spec}）` : '';
	return `${item.productName}${spec} · 余量 ${formatQty(item.totalRemaining)} ${item.unit}`;
}

function getDisabledBatchIds(lines: BatchRowValue[], currentIndex: number): number[] {
	return lines
		.filter((_, index) => index !== currentIndex)
		.map((line) => line.batchId)
		.filter((batchId) => batchId > 0);
}

function isLineOverLimit(line: BatchRowValue, batches: InboundBatch[]): boolean {
	const batch = batches.find((item) => item.inboundId === line.batchId);
	return (
		batch !== undefined && line.qty > 0 && line.qty > batch.remainingQty
	);
}

function calculateSummary(
	lines: BatchRowValue[],
	batches: InboundBatch[],
	saleUnitPrice: number | null | undefined,
	currency: SettlementCurrency,
	exchangeRate: number | null | undefined,
) {
	const activeLines = lines.filter((line) => line.batchId > 0 && line.qty > 0);
	const totalQty = activeLines.reduce((sum, line) => sum + line.qty, 0);
	const weightedCost = activeLines.reduce((sum, line) => {
		const batch = batches.find((item) => item.inboundId === line.batchId);
		return sum + (batch ? line.qty * batch.unitPrice : 0);
	}, 0);
	const price = saleUnitPrice ?? 0;
	const totalSaleAmount = totalQty * price;
	const convertedSaleAmount =
		currency === 'USD' && exchangeRate != null
			? totalSaleAmount * exchangeRate
			: totalSaleAmount;
	const grossProfit = convertedSaleAmount - weightedCost;

	return { totalQty, totalSaleAmount, convertedSaleAmount, weightedCost, grossProfit };
}

interface OutboundSummaryProps {
	lines: BatchRowValue[];
	batches: InboundBatch[];
	unit: string;
}

function OutboundSummary({ lines, batches, unit }: OutboundSummaryProps) {
	const currency = Form.useWatch<SettlementCurrency>('currency') ?? 'CNY';
	const exchangeRate = Form.useWatch<number | null>('exchangeRate');
	const saleUnitPrice = Form.useWatch<number | null>('saleUnitPrice');
	const { totalQty, totalSaleAmount, convertedSaleAmount, weightedCost, grossProfit } =
		calculateSummary(lines, batches, saleUnitPrice, currency, exchangeRate);

	const saleAmountPrefix = currency === 'USD' ? '$' : '¥';

	return (
		<div className={styles.summaryBlock}>
			<div className={styles.summaryRow}>
				<span className={styles.summaryLabel}>总出库数量</span>
				<span className={`${styles.summaryValue} tabular-nums`}>
					{formatQty(totalQty)} {unit}
				</span>
			</div>
			<div className={styles.summaryRow}>
				<span className={styles.summaryLabel}>总销售额</span>
				<span className={`${styles.summaryValue} tabular-nums`}>
					{saleAmountPrefix} {formatMoney(totalSaleAmount)}
				</span>
			</div>
			{currency === 'USD' ? (
				<div className={styles.summaryRow}>
					<span className={styles.summaryLabel}>折算人民币销售额</span>
					<span className={`${styles.summaryValue} tabular-nums`}>
						¥ {formatMoney(convertedSaleAmount)}
					</span>
				</div>
			) : null}
			<div className={styles.summaryRow}>
				<span className={styles.summaryLabel}>加权采购成本</span>
				<span className={`${styles.summaryValue} tabular-nums`}>¥ {formatMoney(weightedCost)}</span>
			</div>
			<div className={styles.summaryRow}>
				<span className={styles.summaryLabel}>预估总毛利</span>
				<span
					className={`${styles.summaryValue} tabular-nums ${
						grossProfit >= 0 ? styles.profitPositive : styles.profitNegative
					}`}
				>
					¥ {formatMoney(grossProfit)}
				</span>
			</div>
		</div>
	);
}

export default function OutboundEntryPage() {
	const navigate = useNavigate();
	const { showToast } = useToast();
	const showAlert = useAlertStore((state) => state.showAlert);
	const [form] = Form.useForm<OutboundFormValues>();
	const [stockItems, setStockItems] = useState<StockItem[]>([]);
	const [customers, setCustomers] = useState<Customer[]>([]);
	const [batches, setBatches] = useState<InboundBatch[]>([]);
	const [batchLines, setBatchLines] = useState<BatchRowValue[]>([EMPTY_BATCH_LINE]);
	const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
	const [optionsLoading, setOptionsLoading] = useState(true);
	const [batchesLoading, setBatchesLoading] = useState(false);
	const [exchangeRateLoading, setExchangeRateLoading] = useState(false);
	const [exchangeRateFetchFailed, setExchangeRateFetchFailed] = useState(false);

	const currency = Form.useWatch('currency', form) ?? 'CNY';
	const outboundDate = Form.useWatch('outboundDate', form);

	const selectedStock = stockItems.find((item) => item.productId === selectedProductId);
	const selectedUnit = selectedStock?.unit ?? batches[0]?.unit ?? '';

	const loadBatches = useCallback(
		async (productId: number) => {
			setBatchesLoading(true);
			try {
				const batchList = await listInboundBatches(productId);
				setBatches(batchList);
			} catch (error) {
				setBatches([]);
				showAlert(getInventoryErrorMessage(error));
			} finally {
				setBatchesLoading(false);
			}
		},
		[showAlert],
	);

	useEffect(() => {
		const loadOptions = async () => {
			setOptionsLoading(true);
			try {
				const [stockResult, customerResult] = await Promise.all([
					listStock({ page: 1, size: DROPDOWN_PAGE_SIZE }),
					listCustomers({ status: 'ACTIVE', page: 1, size: DROPDOWN_PAGE_SIZE }),
				]);
				setStockItems(stockResult.list.filter((item) => item.totalRemaining > 0));
				setCustomers(customerResult.list);
			} catch (error) {
				showAlert(getInventoryErrorMessage(error));
			} finally {
				setOptionsLoading(false);
			}
		};

		loadOptions();
	}, [showAlert]);

	useEffect(() => {
		if (currency !== 'USD') {
			setExchangeRateLoading(false);
			setExchangeRateFetchFailed(false);
			return;
		}

		let cancelled = false;

		const fetchExchangeRate = async () => {
			setExchangeRateLoading(true);
			setExchangeRateFetchFailed(false);

			try {
				const dateStr = outboundDate?.format('YYYY-MM-DD');
				const result = await getCnyUsdExchangeRate(dateStr);

				if (cancelled) {
					return;
				}

				if (result.success && result.rate != null) {
					form.setFieldValue('exchangeRate', result.rate);
					setExchangeRateFetchFailed(false);
				} else {
					form.setFieldValue('exchangeRate', null);
					setExchangeRateFetchFailed(true);
				}
			} catch {
				if (!cancelled) {
					form.setFieldValue('exchangeRate', null);
					setExchangeRateFetchFailed(true);
				}
			} finally {
				if (!cancelled) {
					setExchangeRateLoading(false);
				}
			}
		};

		void fetchExchangeRate();

		return () => {
			cancelled = true;
		};
	}, [currency, outboundDate, form]);

	const stockOptions = useMemo(
		() =>
			stockItems.map((item) => ({
				value: item.productId,
				label: formatStockLabel(item),
			})),
		[stockItems],
	);

	const customerOptions = useMemo(
		() =>
			customers.map((customer) => ({
				value: customer.id,
				label: customer.name,
			})),
		[customers],
	);

	const selectedBatchIds = useMemo(
		() => new Set(batchLines.map((line) => line.batchId).filter((batchId) => batchId > 0)),
		[batchLines],
	);

	const allBatchesAdded =
		batches.length > 0 && batches.every((batch) => selectedBatchIds.has(batch.inboundId));

	const handleProductChange = async (productId: number) => {
		setSelectedProductId(productId);
		setBatchLines([EMPTY_BATCH_LINE]);
		await loadBatches(productId);
	};

	const handleCurrencyChange = (nextCurrency: SettlementCurrency) => {
		if (nextCurrency === 'CNY') {
			form.setFieldValue('exchangeRate', null);
			setExchangeRateFetchFailed(false);
		}
	};

	const handleBatchLineChange = (index: number, value: BatchRowValue) => {
		setBatchLines((lines) => lines.map((line, lineIndex) => (lineIndex === index ? value : line)));
	};

	const handleAddBatchLine = () => {
		setBatchLines((lines) => [...lines, { ...EMPTY_BATCH_LINE }]);
	};

	const handleDeleteBatchLine = (index: number) => {
		setBatchLines((lines) => lines.filter((_, lineIndex) => lineIndex !== index));
	};

	const handleSubmit = async (values: Record<string, unknown>) => {
		const formValues = values as unknown as OutboundFormValues;
		const validLines = batchLines.filter((line) => line.batchId > 0 && line.qty > 0);

		if (validLines.length === 0) {
			showAlert('请至少添加一行批次明细');
			throw new Error('validation');
		}

		if (batchLines.some((line) => isLineOverLimit(line, batches))) {
			showAlert('存在超量批次行，请修正后再提交');
			throw new Error('validation');
		}

		if (formValues.currency === 'USD' && (formValues.exchangeRate == null || formValues.exchangeRate <= 0)) {
			showAlert('请填写汇率');
			throw new Error('validation');
		}

		try {
			await createOutbound({
				productId: formValues.productId,
				customerId: formValues.customerId,
				outboundDate: formValues.outboundDate.format('YYYY-MM-DD'),
				currency: formValues.currency,
				exchangeRate:
					formValues.currency === 'USD' && formValues.exchangeRate != null
						? formValues.exchangeRate
						: undefined,
				saleUnitPrice: formValues.saleUnitPrice,
				remark: formValues.remark?.trim() || undefined,
				createReceivable: formValues.createReceivable,
				batchLines: validLines.map((line) => ({
					inboundId: line.batchId,
					qty: line.qty,
				})),
			});
			showToast('出库记录已保存', 'success');
			navigate('/inventory/stock');
		} catch (error) {
			if (error instanceof Error && error.message === 'validation') {
				throw error;
			}
			showAlert(getInventoryErrorMessage(error));
			throw error;
		}
	};

	const priceAddon = currency === 'USD' ? '$' : '¥';

	return (
		<EntryForm
			title="录入出库"
			submitLabel="提交出库"
			form={form}
			loading={optionsLoading || batchesLoading}
			onCancel={() => navigate('/inventory/stock')}
			onSubmit={handleSubmit}
		>
			<Form.Item
				label="产品"
				name="productId"
				rules={[{ required: true, message: '请选择产品' }]}
			>
				<Select
					placeholder="请选择有库存的产品"
					options={stockOptions}
					showSearch={stockOptions.length > 8}
					optionFilterProp="label"
					onChange={(productId) => handleProductChange(productId)}
				/>
			</Form.Item>

			<Form.Item label="币种" name="currency" initialValue="CNY">
				<Radio.Group onChange={(event) => handleCurrencyChange(event.target.value)}>
					<Radio value="CNY">人民币</Radio>
					<Radio value="USD">美元</Radio>
				</Radio.Group>
			</Form.Item>

			{currency === 'USD' ? (
				<Form.Item
					label="汇率"
					name="exchangeRate"
					rules={[
						{
							validator: async (_, value: number | null | undefined) => {
								if (value == null || value <= 0) {
									throw new Error('请填写汇率');
								}
							},
						},
					]}
				>
					<ExchangeRateInput
						loading={exchangeRateLoading}
						autoFetchFailed={exchangeRateFetchFailed}
					/>
				</Form.Item>
			) : null}

			<div className={styles.fieldRow}>
				<Form.Item
					label="销售单价"
					name="saleUnitPrice"
					rules={[
						{ required: true, message: '请输入销售单价' },
						{ type: 'number', min: 0, message: '单价不能为负' },
					]}
				>
					<InputNumber
						min={0}
						precision={2}
						style={{ width: '100%' }}
						placeholder="请输入单价"
						addonBefore={priceAddon}
					/>
				</Form.Item>

				<Form.Item
					label="出库日期"
					name="outboundDate"
					initialValue={dayjs()}
					rules={[{ required: true, message: '请选择出库日期' }]}
				>
					<DatePicker style={{ width: '100%' }} />
				</Form.Item>
			</div>

			<Form.Item
				label="客户"
				name="customerId"
				rules={[{ required: true, message: '请选择客户' }]}
			>
				<Select
					placeholder="请选择客户"
					options={customerOptions}
					showSearch={customerOptions.length > 8}
					optionFilterProp="label"
				/>
			</Form.Item>

			<div className={styles.batchSection}>
				<p className={styles.batchHeader}>
					批次明细 <span className={styles.requiredMark}>*</span>
				</p>
				<p className={styles.batchHint}>出库成本按所选批次的采购单价计算</p>

				{selectedProductId === null ? (
					<p className={styles.emptyBatchHint}>请先选择产品后添加批次明细</p>
				) : batches.length === 0 && !batchesLoading ? (
					<p className={styles.emptyBatchHint}>该产品暂无可用批次</p>
				) : (
					<>
						<div className={styles.batchList}>
							{batchLines.map((line, index) => (
								<BatchRow
									key={index}
									index={index}
									batches={batches}
									disabledBatchIds={getDisabledBatchIds(batchLines, index)}
									value={line}
									onChange={(value) => handleBatchLineChange(index, value)}
									onDelete={() => handleDeleteBatchLine(index)}
									canDelete={batchLines.length > 1}
								/>
							))}
						</div>
						<Button
							variant="secondary"
							size="compact"
							disabled={allBatchesAdded || batches.length === 0}
							onClick={handleAddBatchLine}
						>
							{allBatchesAdded ? '该产品所有批次已添加' : '+ 添加批次'}
						</Button>
					</>
				)}
			</div>

			{selectedProductId !== null && batches.length > 0 ? (
				<OutboundSummary lines={batchLines} batches={batches} unit={selectedUnit} />
			) : null}

			<Form.Item name="createReceivable" valuePropName="checked" initialValue={false}>
				<Checkbox>该笔货款未收</Checkbox>
			</Form.Item>

			<Form.Item label="备注" name="remark">
				<Input.TextArea placeholder="选填" rows={3} />
			</Form.Item>
		</EntryForm>
	);
}
