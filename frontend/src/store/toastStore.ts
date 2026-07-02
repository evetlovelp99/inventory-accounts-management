import { create } from 'zustand';

export type ToastType = 'success' | 'error';

export interface ToastItem {
	id: string;
	message: string;
	type: ToastType;
}

const MAX_TOASTS = 3;

let toastIdCounter = 0;

interface ToastState {
	toasts: ToastItem[];
	showToast: (message: string, type: ToastType) => void;
	dismissToast: (id: string) => void;
}

export const useToastStore = create<ToastState>((set) => ({
	toasts: [],
	showToast: (message, type) => {
		const id = String(++toastIdCounter);
		set((state) => ({
			toasts: [...state.toasts, { id, message, type }].slice(-MAX_TOASTS),
		}));
	},
	dismissToast: (id) => {
		set((state) => ({
			toasts: state.toasts.filter((toast) => toast.id !== id),
		}));
	},
}));
