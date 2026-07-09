import axios from 'axios';
import apiClient, { type ApiResponse } from './client';
import type { InboundBatch } from '../components/EntryForm/batchTypes';
import type { PageResult } from './settings';

export interface InboundProductionInfo {
	originPlace?: string;
	harvestDate?: string;
	inspectNo?: string;
	inspectOrg?: string;
	inspectDate?: string;
	inspectFileUrl?: string;
	expiryDate?: string;
}

export interface InboundCreatePayload {
	productId: number;
	supplierId: number;
	inboundDate: string;
	quantity: number;
	unitPrice: number;
	remark?: string;
	createPayable?: boolean;
	originPlace?: string;
	harvestDate?: string;
	inspectNo?: string;
	inspectOrg?: string;
	inspectDate?: string;
	inspectFileUrl?: string;
	expiryDate?: string;
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

export async function uploadInspectReport(file: File): Promise<string> {
	const formData = new FormData();
	formData.append('file', file);
	const response = await apiClient.post<ApiResponse<{ url: string }>>(
		'/files/inspect-reports',
		formData,
		{
			headers: { 'Content-Type': 'multipart/form-data' },
		},
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data.url;
}

export async function downloadInspectReport(url: string, filename: string): Promise<void> {
	const path = url.startsWith('/api/') ? url.slice(4) : url;
	const response = await apiClient.get(path, { responseType: 'blob' });
	const blobUrl = window.URL.createObjectURL(response.data);
	const link = document.createElement('a');
	link.href = blobUrl;
	link.download = filename;
	link.click();
	window.URL.revokeObjectURL(blobUrl);
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

export type SettlementCurrency = 'CNY' | 'USD';

export interface ExchangeRateResult {
	success: boolean;
	rate: number | null;
	date?: string;
	source?: string;
	message?: string;
}

export interface OutboundCreatePayload {
	productId: number;
	customerId: number;
	outboundDate: string;
	currency?: SettlementCurrency;
	exchangeRate?: number;
	saleUnitPrice: number;
	remark?: string;
	createReceivable?: boolean;
	batchLines: OutboundBatchLinePayload[];
}

export interface OutboundCreateResult {
	outboundId: number;
	totalQty: number;
	currency: SettlementCurrency;
	totalSaleAmount: number;
	convertedSaleAmount: number;
	weightedCost: number;
	grossProfit: number;
	receivableId: number | null;
}

export async function getCnyUsdExchangeRate(date?: string): Promise<ExchangeRateResult> {
	const response = await apiClient.get<ExchangeRateResult>('/inventory/exchange-rate/cny-usd', {
		params: date ? { date } : undefined,
	});

	return response.data;
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

export type LedgerEntryType = 'INBOUND' | 'OUTBOUND';

export interface LedgerEntry {
	id: number;
	type: LedgerEntryType;
	date: string;
	qty: number;
	unitPrice: number;
	amount: number;
	currency?: SettlementCurrency;
	partyName: string;
	remark: string | null;
	productionInfo: InboundProductionInfo | null;
}

export interface ProductLedgerResult {
	productName: string;
	unit: string;
	list: LedgerEntry[];
	total: number;
}

export interface ProductLedgerParams {
	startDate?: string;
	endDate?: string;
	page?: number;
	size?: number;
}

export async function getProductLedger(
	productId: number,
	params: ProductLedgerParams = {},
): Promise<ProductLedgerResult> {
	const response = await apiClient.get<ApiResponse<ProductLedgerResult>>(
		`/inventory/stock/${productId}/ledger`,
		{ params },
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}
