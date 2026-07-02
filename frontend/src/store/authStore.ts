import { create } from 'zustand';
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client';

export type UserRole = 'OWNER' | 'FINANCE' | 'WAREHOUSE' | 'SUPERVISOR';

export interface AuthUser {
	id: number;
	name: string;
	role: UserRole;
}

interface AuthState {
	token: string | null;
	user: AuthUser | null;
	login: (token: string, user: AuthUser) => void;
	logout: () => void;
}

function loadToken(): string | null {
	return localStorage.getItem(AUTH_TOKEN_KEY);
}

function loadUser(): AuthUser | null {
	const raw = localStorage.getItem(AUTH_USER_KEY);
	if (!raw) {
		return null;
	}
	try {
		return JSON.parse(raw) as AuthUser;
	} catch {
		return null;
	}
}

export const useAuthStore = create<AuthState>((set) => ({
	token: loadToken(),
	user: loadUser(),
	login: (token, user) => {
		localStorage.setItem(AUTH_TOKEN_KEY, token);
		localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
		set({ token, user });
	},
	logout: () => {
		localStorage.removeItem(AUTH_TOKEN_KEY);
		localStorage.removeItem(AUTH_USER_KEY);
		set({ token: null, user: null });
	},
}));
