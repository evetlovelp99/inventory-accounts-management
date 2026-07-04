import axios from 'axios';
import apiClient, { type ApiResponse } from './client';
import type { InboundBatch } from '../components/EntryForm/batchTypes';
import type { PageResult } from './settings';

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

export interface StockItem {
	productId: number;
	productName: string;
	spec: string | null;
	unit: string;
	totalRemaining: number;
	lastUpdated: string;
}

export interface OutboundBatchLinePayload {
	inboundId: number;
	qty: number;
}

export interface OutboundCreatePayload {
	productId: number;
	customerId: number;
	outboundDate: string;
	saleUnitPrice: number;
	remark?: string;
	createReceivable?: boolean;
	batchLines: OutboundBatchLinePayload[];
}

export interface OutboundCreateResult {
	outboundId: number;
	totalQty: number;
	totalSaleAmount: number;
	weightedCost: number;
	grossProfit: number;
	receivableId: number | null;
}

export async function listStock(params: {
	keyword?: string;
	page?: number;
	size?: number;
} = {}): Promise<PageResult<StockItem>> {
	const response = await apiClient.get<ApiResponse<PageResult<StockItem>>>(
		'/inventory/stock',
		{ params },
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function listInboundBatches(productId: number): Promise<InboundBatch[]> {
	const response = await apiClient.get<ApiResponse<InboundBatch[]>>(
		`/inventory/inbound/${productId}/batches`,
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function createOutbound(payload: OutboundCreatePayload): Promise<OutboundCreateResult> {
	const response = await apiClient.post<ApiResponse<OutboundCreateResult>>(
		'/inventory/outbound',
		payload,
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}
