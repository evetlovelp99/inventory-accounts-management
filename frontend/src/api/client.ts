import axios from 'axios';

/** localStorage key for JWT; shared with authStore (step 0.15) */
export const AUTH_TOKEN_KEY = 'beewax_auth_token';

export interface ApiResponse<T> {
	code: number;
	message: string;
	data: T;
}

const baseURL =
	import.meta.env.VITE_API_BASE_URL ??
	(import.meta.env.DEV ? 'http://localhost:8080/api' : '/api');

export const apiClient = axios.create({
	baseURL,
	headers: {
		'Content-Type': 'application/json',
	},
});

apiClient.interceptors.request.use((config) => {
	const token = localStorage.getItem(AUTH_TOKEN_KEY);
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});

apiClient.interceptors.response.use(
	(response) => response,
	(error) => {
		if (error.response?.status === 401) {
			localStorage.removeItem(AUTH_TOKEN_KEY);
			const loginPath = '/login';
			if (window.location.pathname !== loginPath) {
				window.location.assign(loginPath);
			}
		}
		return Promise.reject(error);
	},
);

export default apiClient;
