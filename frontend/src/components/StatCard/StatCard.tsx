import type { ReactNode } from 'react';
import styles from './StatCard.module.css';

export type StatCardStatus = 'normal' | 'warning' | 'success';

export interface StatCardProps {
	label: string;
	value: ReactNode;
	unit?: string;
	trend?: string;
	status?: StatCardStatus;
	loading?: boolean;
	onClick?: () => void;
}

export default function StatCard({
	label,
	value,
	unit,
	trend,
	status = 'normal',
	loading = false,
	onClick,
}: StatCardProps) {
	const statusClass =
		status === 'warning'
			? styles.warning
			: status === 'success'
				? styles.success
				: '';

	if (loading) {
		return (
			<div className={`${styles.card} ${styles.loading}`} aria-busy="true">
				<div className={styles.skeletonLabel} />
				<div className={styles.skeletonValue} />
			</div>
		);
	}

	return (
		<div
			className={`${styles.card} ${statusClass} ${onClick ? styles.clickable : ''}`}
			onClick={onClick}
			onKeyDown={
				onClick
					? (event) => {
							if (event.key === 'Enter' || event.key === ' ') {
								event.preventDefault();
								onClick();
							}
						}
					: undefined
			}
			role={onClick ? 'button' : undefined}
			tabIndex={onClick ? 0 : undefined}
		>
			<p className={styles.label}>{label}</p>
			<p className={`${styles.value} tabular-nums`}>{value}</p>
			{unit || trend ? (
				<p className={styles.meta}>
					{trend ? <span>{trend}</span> : null}
					{unit ? <span>{unit}</span> : null}
				</p>
			) : null}
		</div>
	);
}
