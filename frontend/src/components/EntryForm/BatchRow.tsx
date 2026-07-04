import type { BatchRowValue, InboundBatch } from './batchTypes';
import styles from './BatchRow.module.css';

export type { BatchRowValue, InboundBatch } from './batchTypes';

export interface BatchRowProps {
	index: number;
	batches: InboundBatch[];
	disabledBatchIds: number[];
	value: BatchRowValue;
	onChange: (value: BatchRowValue) => void;
	onDelete: () => void;
	canDelete: boolean;
}

function formatInboundDate(dateStr: string): string {
	const [, month, day] = dateStr.split('-');
	return `${month}-${day}`;
}

function formatUnitPrice(price: number): string {
	return `¥${price.toFixed(2)}`;
}

function formatBatchOptionLabel(batch: InboundBatch): string {
	return `${batch.inboundId} — 余量 ${batch.remainingQty} ${batch.unit} — 采购价 ${formatUnitPrice(batch.unitPrice)}`;
}

function isBatchDisabled(
	batch: InboundBatch,
	disabledBatchIds: number[],
	selectedBatchId: number,
): boolean {
	return (
		disabledBatchIds.includes(batch.inboundId) &&
		batch.inboundId !== selectedBatchId
	);
}

export default function BatchRow({
	index,
	batches,
	disabledBatchIds,
	value,
	onChange,
	onDelete,
	canDelete,
}: BatchRowProps) {
	const selectedBatch = batches.find((batch) => batch.inboundId === value.batchId);
	const isOverLimit =
		selectedBatch !== undefined &&
		value.qty > 0 &&
		value.qty > selectedBatch.remainingQty;

	const handleBatchChange = (batchId: number) => {
		onChange({ batchId, qty: value.qty });
	};

	const handleQtyChange = (rawQty: string) => {
		const qty = rawQty === '' ? 0 : Number.parseFloat(rawQty);
		onChange({
			batchId: value.batchId,
			qty: Number.isNaN(qty) ? 0 : qty,
		});
	};

	return (
		<div className={styles.row} aria-label={`批次明细行 ${index + 1}`}>
			<div className={styles.mainRow}>
				<select
					className={styles.batchSelect}
					value={value.batchId || ''}
					onChange={(event) => handleBatchChange(Number(event.target.value))}
					aria-label={`选择入库批次 ${index + 1}`}
				>
					<option value="" disabled>
						选择批次
					</option>
					{batches.map((batch) => (
						<option
							key={batch.inboundId}
							value={batch.inboundId}
							disabled={isBatchDisabled(
								batch,
								disabledBatchIds,
								value.batchId,
							)}
						>
							{formatBatchOptionLabel(batch)}
						</option>
					))}
				</select>

				<input
					type="number"
					className={`${styles.qtyInput} tabular-nums ${isOverLimit ? styles.qtyInputError : ''}`}
					value={value.qty === 0 ? '' : value.qty}
					min={0}
					step="any"
					placeholder="数量"
					onChange={(event) => handleQtyChange(event.target.value)}
					aria-label={`出库数量 ${index + 1}`}
					aria-invalid={isOverLimit}
				/>

				<button
					type="button"
					className={styles.deleteButton}
					onClick={onDelete}
					disabled={!canDelete}
					aria-label={`删除批次行 ${index + 1}`}
				>
					×
				</button>
			</div>

			{selectedBatch ? (
				<p className={styles.batchInfo}>
					入库日期 {formatInboundDate(selectedBatch.inboundDate)} · 采购价{' '}
					{formatUnitPrice(selectedBatch.unitPrice)}/{selectedBatch.unit} · 余量{' '}
					{selectedBatch.remainingQty} {selectedBatch.unit}
				</p>
			) : null}

			{isOverLimit && selectedBatch ? (
				<p className={styles.error} role="alert">
					⚠ 不能超过该批次余量 {selectedBatch.remainingQty} {selectedBatch.unit}
				</p>
			) : null}
		</div>
	);
}
