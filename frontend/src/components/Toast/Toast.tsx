import { useEffect, useState } from 'react';
import { useToastStore, type ToastItem } from '../../store/toastStore';
import styles from './Toast.module.css';

const EXIT_ANIMATION_MS = 200;
const SUCCESS_DURATION_MS = 3000;

function ToastMessage({ toast, onDismiss }: { toast: ToastItem; onDismiss: (id: string) => void }) {
	const [exiting, setExiting] = useState(false);

	const handleDismiss = () => {
		if (exiting) {
			return;
		}
		setExiting(true);
		window.setTimeout(() => onDismiss(toast.id), EXIT_ANIMATION_MS);
	};

	useEffect(() => {
		if (toast.type !== 'success') {
			return;
		}
		const timer = window.setTimeout(() => {
			setExiting(true);
			window.setTimeout(() => onDismiss(toast.id), EXIT_ANIMATION_MS);
		}, SUCCESS_DURATION_MS);
		return () => window.clearTimeout(timer);
	}, [toast.id, toast.type, onDismiss]);

	return (
		<div
			className={`${styles.toast} ${exiting ? styles.exiting : ''}`}
			role={toast.type === 'error' ? 'alert' : 'status'}
			aria-live={toast.type === 'error' ? 'assertive' : 'polite'}
		>
			<span className={styles.icon} aria-hidden>
				{toast.type === 'success' ? '✓' : '✕'}
			</span>
			<p className={styles.message}>{toast.message}</p>
			{toast.type === 'error' ? (
				<button
					type="button"
					className={styles.closeButton}
					onClick={handleDismiss}
					aria-label="关闭提示"
				>
					×
				</button>
			) : null}
		</div>
	);
}

export default function Toast() {
	const toasts = useToastStore((state) => state.toasts);
	const dismissToast = useToastStore((state) => state.dismissToast);

	if (toasts.length === 0) {
		return null;
	}

	return (
		<div className={styles.container}>
			{toasts.map((toast) => (
				<ToastMessage key={toast.id} toast={toast} onDismiss={dismissToast} />
			))}
		</div>
	);
}
