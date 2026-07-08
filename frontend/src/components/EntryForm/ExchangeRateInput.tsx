import { InputNumber, Spin } from 'antd';
import styles from './ExchangeRateInput.module.css';

export interface ExchangeRateInputProps {
	value: number | null;
	onChange: (rate: number) => void;
	loading: boolean;
	autoFetchFailed: boolean;
}

export default function ExchangeRateInput({
	value,
	onChange,
	loading,
	autoFetchFailed,
}: ExchangeRateInputProps) {
	return (
		<div className={styles.wrapper}>
			<div className={styles.inputShell}>
				<InputNumber
					min={0}
					precision={4}
					value={value}
					disabled={loading}
					placeholder={loading ? '获取汇率中…' : '请输入汇率'}
					className={styles.input}
					addonBefore="¥"
					onChange={(nextValue) => {
						if (nextValue !== null) {
							onChange(nextValue);
						}
					}}
				/>
				{loading ? (
					<div className={styles.loadingOverlay} aria-hidden="true">
						<Spin size="small" />
					</div>
				) : null}
			</div>
			{autoFetchFailed ? (
				<p className={styles.fetchFailedHint} role="status">
					自动获取汇率失败，请手动输入当日汇率
				</p>
			) : null}
		</div>
	);
}
