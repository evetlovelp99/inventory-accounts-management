import axios from 'axios';
import apiClient, { type ApiResponse } from './client';

export interface InboundCreatePayload {
	productId: number;
	supplierId: number;
	inboundDate: string;
	quantity: number;
	unitPrice: number;
	remark?: string;
}

export interface InboundCreateResult {
	inboundId: number;
	totalAmount: number;
	payableId: number | null;
}

export function getInventoryErrorMessage(error: unknown): string {
	if (axios.isAxiosError(error)) {
		const message = (error.response?.data as { message?: string } | undefined)?.message;
		if (message) {
			return message;
		}
	}
	if (error instanceof Error) {
		return error.message;
	}
	return '提交失败，请稍后重试';
}

export async function createInbound(payload: InboundCreatePayload): Promise<InboundCreateResult> {
	const response = await apiClient.post<ApiResponse<InboundCreateResult>>(
		'/inventory/inbound',
		payload,
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}
