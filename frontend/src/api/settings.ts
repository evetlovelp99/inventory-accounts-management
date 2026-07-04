import axios from 'axios';
import apiClient, { type ApiResponse } from './client';

export type ProductStatus = 'ACTIVE' | 'INACTIVE';

export interface Product {
	id: number;
	name: string;
	spec: string | null;
	unit: string;
	status: ProductStatus;
}

export interface PageResult<T> {
	list: T[];
	total: number;
	page: number;
	size: number;
}

export interface ProductCreatePayload {
	name: string;
	spec?: string;
	unit: string;
}

export interface ProductUpdatePayload {
	name: string;
	spec?: string;
	unit: string;
}

export interface ListProductsParams {
	keyword?: string;
	status?: ProductStatus;
	page?: number;
	size?: number;
}

const DEACTIVATE_WARNING_MESSAGE = '该产品仍有余量，停用后将不再显示在录入选项中';

export function getSettingsErrorMessage(error: unknown): string {
	if (axios.isAxiosError(error)) {
		const message = (error.response?.data as { message?: string } | undefined)?.message;
		if (message) {
			return message;
		}
	}
	if (error instanceof Error) {
		return error.message;
	}
	return '操作失败，请稍后重试';
}

export function isDeactivateWarningError(error: unknown): boolean {
	return getSettingsErrorMessage(error) === DEACTIVATE_WARNING_MESSAGE;
}

export async function listProducts(
	params: ListProductsParams = {},
): Promise<PageResult<Product>> {
	const response = await apiClient.get<ApiResponse<PageResult<Product>>>(
		'/settings/products',
		{ params },
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function createProduct(payload: ProductCreatePayload): Promise<Product> {
	const response = await apiClient.post<ApiResponse<Product>>('/settings/products', payload);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function updateProduct(
	id: number,
	payload: ProductUpdatePayload,
): Promise<Product> {
	const response = await apiClient.put<ApiResponse<Product>>(
		`/settings/products/${id}`,
		payload,
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function updateProductStatus(
	id: number,
	status: ProductStatus,
	force = false,
): Promise<Product> {
	const response = await apiClient.put<ApiResponse<Product>>(
		`/settings/products/${id}/status`,
		{ status, force: force || undefined },
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}
