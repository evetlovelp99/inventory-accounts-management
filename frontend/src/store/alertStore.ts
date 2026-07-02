import { create } from 'zustand';

export interface AlertStateItem {
	message: string;
	visible: boolean;
}

interface AlertState {
	alert: AlertStateItem | null;
	showAlert: (message: string) => void;
	hideAlert: () => void;
}

export const useAlertStore = create<AlertState>((set) => ({
	alert: null,
	showAlert: (message) => {
		set({ alert: { message, visible: true } });
	},
	hideAlert: () => {
		set({ alert: null });
	},
}));
