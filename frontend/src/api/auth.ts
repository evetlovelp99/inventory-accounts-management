import axios from 'axios';
import apiClient, { type ApiResponse } from './client';
import type { AuthUser } from '../store/authStore';

export interface LoginData {
	token: string;
	user: AuthUser;
}

export async function login(username: string, password: string): Promise<LoginData> {
	const response = await apiClient.post<ApiResponse<LoginData>>('/auth/login', {
		username,
		password,
	});

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export function getLoginErrorMessage(error: unknown): string {
	if (axios.isAxiosError(error)) {
		const message = (error.response?.data as { message?: string } | undefined)?.message;
		if (message) {
			return message;
		}
	}
	if (error instanceof Error) {
		return error.message;
	}
	return '登录失败，请稍后重试';
}
