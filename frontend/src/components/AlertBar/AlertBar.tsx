import { useAlertStore } from '../../store/alertStore';
import styles from './AlertBar.module.css';

export default function AlertBar() {
	const alert = useAlertStore((state) => state.alert);
	const hideAlert = useAlertStore((state) => state.hideAlert);

	if (!alert?.visible) {
		return null;
	}

	return (
		<div className={styles.bar} role="alert" aria-live="assertive">
			<p className={styles.message}>⚠ {alert.message}</p>
			<button type="button" className={styles.closeButton} onClick={hideAlert} aria-label="关闭警告">
				×
			</button>
		</div>
	);
}
