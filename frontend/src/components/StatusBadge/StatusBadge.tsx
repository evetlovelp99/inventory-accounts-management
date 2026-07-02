import styles from './StatusBadge.module.css';

export type StatusBadgeStatus =
	| '已结清'
	| '未结清'
	| '部分还款'
	| '入库'
	| '出库'
	| '还款记录';

interface StatusStyle {
	backgroundColor: string;
	color: string;
	dotColor?: string;
	showDot: boolean;
}

const STATUS_STYLES: Record<StatusBadgeStatus, StatusStyle> = {
	已结清: {
		backgroundColor: 'var(--color-grove-light)',
		color: 'var(--color-grove)',
		dotColor: 'var(--color-grove)',
		showDot: true,
	},
	未结清: {
		backgroundColor: 'var(--color-brick-light)',
		color: 'var(--color-brick)',
		dotColor: 'var(--color-brick)',
		showDot: true,
	},
	部分还款: {
		backgroundColor: 'var(--color-clay-light)',
		color: 'var(--color-clay)',
		dotColor: 'var(--color-clay)',
		showDot: true,
	},
	入库: {
		backgroundColor: 'var(--color-amber-light)',
		color: 'var(--color-amber)',
		showDot: false,
	},
	出库: {
		backgroundColor: '#EDF2FF',
		color: '#3B5BDB',
		showDot: false,
	},
	还款记录: {
		backgroundColor: '#F3F0FF',
		color: '#6741D9',
		showDot: false,
	},
};

export interface StatusBadgeProps {
	status: StatusBadgeStatus;
}

export default function StatusBadge({ status }: StatusBadgeProps) {
	const styleConfig = STATUS_STYLES[status];

	return (
		<span
			className={styles.badge}
			style={{
				backgroundColor: styleConfig.backgroundColor,
				color: styleConfig.color,
			}}
		>
			{styleConfig.showDot && (
				<span
					className={styles.dot}
					style={{ backgroundColor: styleConfig.dotColor }}
					aria-hidden
				/>
			)}
			<span className={styles.label}>{status}</span>
		</span>
	);
}
