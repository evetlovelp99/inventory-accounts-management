import { Button, Collapse, DatePicker, Form, Input, InputNumber, Select, Upload } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import dayjs, { type Dayjs } from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
	createInbound,
	getInventoryErrorMessage,
	uploadInspectReport,
} from '../../api/inventory';
import {
	listProducts,
	listSuppliers,
	type Product,
	type Supplier,
} from '../../api/settings';
import EntryForm from '../../components/EntryForm/EntryForm';
import { useToast } from '../../hooks/useToast';
import { useAlertStore } from '../../store/alertStore';
import styles from './InboundEntryPage.module.css';

interface InboundFormValues {
	productId: number;
	supplierId: number;
	inboundDate: Dayjs;
	quantity: number;
	unitPrice: number;
	remark?: string;
	originPlace?: string;
	harvestDate?: Dayjs;
	inspectNo?: string;
	inspectOrg?: string;
	inspectDate?: Dayjs;
	inspectFileUrl?: string;
	expiryDate?: Dayjs;
}

const DROPDOWN_PAGE_SIZE = 500;
const ACCEPTED_FILE_TYPES = '.pdf,.jpg,.jpeg,.png';

function formatMoney(amount: number): string {
	return amount.toLocaleString('zh-CN', {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2,
	});
}

function formatProductLabel(product: Product): string {
	const spec = product.spec ? `（${product.spec}）` : '';
	return `${product.name}${spec} · ${product.unit}`;
}

function formatDateValue(value?: Dayjs): string | undefined {
	return value ? value.format('YYYY-MM-DD') : undefined;
}

function InboundTotalDisplay() {
	const quantity = Form.useWatch<number | null>('quantity');
	const unitPrice = Form.useWatch<number | null>('unitPrice');
	const total =
		quantity !== null &&
		quantity !== undefined &&
		unitPrice !== null &&
		unitPrice !== undefined &&
		quantity > 0 &&
		unitPrice >= 0
			? quantity * unitPrice
			: 0;

	return (
		<div className={styles.totalBlock}>
			<p className={styles.totalLabel}>本批总金额（自动计算）</p>
			<p className={`${styles.totalValue} tabular-nums`}>¥ {formatMoney(total)}</p>
		</div>
	);
}

export default function InboundEntryPage() {
	const navigate = useNavigate();
	const { showToast } = useToast();
	const showAlert = useAlertStore((state) => state.showAlert);
	const [form] = Form.useForm<InboundFormValues>();
	const [products, setProducts] = useState<Product[]>([]);
	const [suppliers, setSuppliers] = useState<Supplier[]>([]);
	const [optionsLoading, setOptionsLoading] = useState(true);
	const [inspectFileList, setInspectFileList] = useState<UploadFile[]>([]);
	const [inspectUploading, setInspectUploading] = useState(false);

	useEffect(() => {
		const loadOptions = async () => {
			setOptionsLoading(true);
			try {
				const [productResult, supplierResult] = await Promise.all([
					listProducts({ status: 'ACTIVE', page: 1, size: DROPDOWN_PAGE_SIZE }),
					listSuppliers({ status: 'ACTIVE', page: 1, size: DROPDOWN_PAGE_SIZE }),
				]);
				setProducts(productResult.list);
				setSuppliers(supplierResult.list);
			} catch (error) {
				showAlert(getInventoryErrorMessage(error));
			} finally {
				setOptionsLoading(false);
			}
		};

		loadOptions();
	}, [showAlert]);

	const productOptions = useMemo(
		() =>
			products.map((product) => ({
				value: product.id,
				label: formatProductLabel(product),
			})),
		[products],
	);

	const supplierOptions = useMemo(
		() =>
			suppliers.map((supplier) => ({
				value: supplier.id,
				label: supplier.name,
			})),
		[suppliers],
	);

	const handleInspectUpload = async (file: File) => {
		setInspectUploading(true);
		try {
			const url = await uploadInspectReport(file);
			form.setFieldValue('inspectFileUrl', url);
			setInspectFileList([
				{
					uid: url,
					name: file.name,
					status: 'done',
					url,
				},
			]);
		} catch (error) {
			showAlert(getInventoryErrorMessage(error));
			setInspectFileList([]);
			form.setFieldValue('inspectFileUrl', undefined);
		} finally {
			setInspectUploading(false);
		}
	};

	const handleSubmit = async (values: Record<string, unknown>) => {
		const formValues = values as unknown as InboundFormValues;
		try {
			await createInbound({
				productId: formValues.productId,
				supplierId: formValues.supplierId,
				inboundDate: formValues.inboundDate.format('YYYY-MM-DD'),
				quantity: formValues.quantity,
				unitPrice: formValues.unitPrice,
				remark: formValues.remark?.trim() || undefined,
				originPlace: formValues.originPlace?.trim() || undefined,
				harvestDate: formatDateValue(formValues.harvestDate),
				inspectNo: formValues.inspectNo?.trim() || undefined,
				inspectOrg: formValues.inspectOrg?.trim() || undefined,
				inspectDate: formatDateValue(formValues.inspectDate),
				inspectFileUrl: formValues.inspectFileUrl || undefined,
				expiryDate: formatDateValue(formValues.expiryDate),
			});
			showToast('入库记录已保存', 'success');
			navigate('/inventory/stock');
		} catch (error) {
			showAlert(getInventoryErrorMessage(error));
			throw error;
		}
	};

	return (
		<EntryForm
			title="录入入库"
			submitLabel="提交入库"
			loading={optionsLoading || inspectUploading}
			form={form}
			onCancel={() => navigate('/inventory/stock')}
			onSubmit={handleSubmit}
		>
			<Form.Item
				label="产品"
				name="productId"
				rules={[{ required: true, message: '请选择产品' }]}
			>
				<Select
					placeholder="请选择产品"
					options={productOptions}
					showSearch={productOptions.length > 8}
					optionFilterProp="label"
				/>
			</Form.Item>

			<div className={styles.fieldRow}>
				<Form.Item shouldUpdate noStyle>
					{({ getFieldValue }) => {
						const productId = getFieldValue('productId') as number | undefined;
						const selectedProduct = products.find((product) => product.id === productId);
						const unitLabel = selectedProduct ? `（${selectedProduct.unit}）` : '';

						return (
							<Form.Item
								label={`入库数量${unitLabel}`}
								name="quantity"
								rules={[
									{ required: true, message: '请输入入库数量' },
									{ type: 'number', min: 0.001, message: '数量必须大于 0' },
								]}
							>
								<InputNumber
									min={0}
									style={{ width: '100%' }}
									placeholder="请输入数量"
								/>
							</Form.Item>
						);
					}}
				</Form.Item>

				<Form.Item
					label="采购单价"
					name="unitPrice"
					rules={[
						{ required: true, message: '请输入采购单价' },
						{ type: 'number', min: 0, message: '单价不能为负' },
					]}
				>
					<InputNumber
						min={0}
						precision={2}
						style={{ width: '100%' }}
						placeholder="请输入单价"
						addonBefore="¥"
					/>
				</Form.Item>
			</div>

			<div className={styles.fieldRow}>
				<Form.Item
					label="入库日期"
					name="inboundDate"
					initialValue={dayjs()}
					rules={[{ required: true, message: '请选择入库日期' }]}
				>
					<DatePicker style={{ width: '100%' }} />
				</Form.Item>

				<Form.Item
					label="供应商"
					name="supplierId"
					rules={[{ required: true, message: '请选择供应商' }]}
				>
					<Select
						placeholder="请选择供应商"
						options={supplierOptions}
						showSearch={supplierOptions.length > 8}
						optionFilterProp="label"
					/>
				</Form.Item>
			</div>

			<InboundTotalDisplay />

			<Form.Item label="备注" name="remark">
				<Input.TextArea placeholder="选填" rows={3} />
			</Form.Item>

			<Collapse
				className={styles.productionCollapse}
				items={[
					{
						key: 'production',
						label: '生产信息（选填）',
						children: (
							<>
								<Form.Item label="产地/蜂场" name="originPlace">
									<Input placeholder="选填" />
								</Form.Item>

								<div className={styles.fieldRow}>
									<Form.Item label="采集日期" name="harvestDate">
										<DatePicker style={{ width: '100%' }} />
									</Form.Item>

									<Form.Item label="保质期至" name="expiryDate">
										<DatePicker style={{ width: '100%' }} />
									</Form.Item>
								</div>

								<div className={styles.fieldRow}>
									<Form.Item label="检测报告编号" name="inspectNo">
										<Input placeholder="选填" />
									</Form.Item>

									<Form.Item label="检测机构" name="inspectOrg">
										<Input placeholder="选填" />
									</Form.Item>
								</div>

								<Form.Item label="检测日期" name="inspectDate">
									<DatePicker style={{ width: '100%' }} />
								</Form.Item>

								<Form.Item label="检测报告" name="inspectFileUrl">
									<Upload
										accept={ACCEPTED_FILE_TYPES}
										maxCount={1}
										fileList={inspectFileList}
										beforeUpload={(file) => {
											void handleInspectUpload(file);
											return false;
										}}
										onRemove={() => {
											setInspectFileList([]);
											form.setFieldValue('inspectFileUrl', undefined);
										}}
									>
										<Button>上传文件</Button>
									</Upload>
									<p className={styles.uploadHint}>支持 PDF、JPG、PNG，最大 10MB</p>
								</Form.Item>
							</>
						),
					},
				]}
			/>
		</EntryForm>
	);
}
