import axios from 'axios';
import apiClient, { type ApiResponse } from './client';
import type { SettlementCurrency } from './inventory';

export type ReceivableStatus = 'UNPAID' | 'PARTIAL' | 'PAID';

export interface ReceivableSummaryItem {
	customerId: number;
	customerName: string;
	currency: SettlementCurrency;
	originalAmount: number;
	convertedAmount: number;
	paidAmount: number;
	remainingAmount: number;
	oldestUnpaidDate: string;
	daysSinceOldest: number;
	status: ReceivableStatus;
}

export interface ReceivableListResult {
	totalUnpaidAmount: number;
	list: ReceivableSummaryItem[];
	total: number;
}

export interface PaymentLogItem {
	id: number;
	amount: number;
	paymentDate: string;
	remark: string | null;
}

export interface ReceivableRecord {
	id: number;
	originalAmount: number;
	paidAmount: number;
	remainingAmount: number;
	occurDate: string;
	status: ReceivableStatus;
	outboundId: number | null;
	remark: string | null;
	paymentLogs: PaymentLogItem[];
}

export interface ReceivableDetailResult {
	customerName: string;
	records: ReceivableRecord[];
}

export interface ReceivablePaymentPayload {
	amount: number;
	paymentDate: string;
	remark?: string;
}

export interface ReceivablePaymentResult {
	remainingAmount: number;
	status: ReceivableStatus;
}

export type PayableStatus = ReceivableStatus;

export interface PayableRecord {
	id: number;
	originalAmount: number;
	paidAmount: number;
	remainingAmount: number;
	occurDate: string;
	status: PayableStatus;
	inboundId: number | null;
	remark: string | null;
	paymentLogs: PaymentLogItem[];
}

export interface PayableDetailResult {
	supplierName: string;
	records: PayableRecord[];
}

export interface PayableSummaryItem {
	supplierId: number;
	supplierName: string;
	originalAmount: number;
	paidAmount: number;
	remainingAmount: number;
	oldestUnpaidDate: string;
	daysSinceOldest: number;
	status: PayableStatus;
}

export interface PayableListResult {
	totalUnpaidAmount: number;
	list: PayableSummaryItem[];
	total: number;
}

export interface PayablePaymentPayload {
	amount: number;
	paymentDate: string;
	remark?: string;
}

export interface PayablePaymentResult {
	remainingAmount: number;
	status: PayableStatus;
}

export function getAccountsErrorMessage(error: unknown): string {
	if (axios.isAxiosError(error)) {
		const message = (error.response?.data as { message?: string } | undefined)?.message;
		if (message) {
			return message;
		}
	}
	if (error instanceof Error) {
		return error.message;
	}
	return '加载失败，请稍后重试';
}

export async function listReceivables(params: {
	keyword?: string;
	status?: ReceivableStatus;
	page?: number;
	size?: number;
} = {}): Promise<ReceivableListResult> {
	const response = await apiClient.get<ApiResponse<ReceivableListResult>>(
		'/accounts/receivable',
		{ params },
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function getReceivableDetail(
	customerId: number,
	params: { startDate?: string; endDate?: string } = {},
): Promise<ReceivableDetailResult> {
	const response = await apiClient.get<ApiResponse<ReceivableDetailResult>>(
		'/accounts/receivable/detail',
		{ params: { customerId, ...params } },
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function registerReceivablePayment(
	id: number,
	payload: ReceivablePaymentPayload,
): Promise<ReceivablePaymentResult> {
	const response = await apiClient.post<ApiResponse<ReceivablePaymentResult>>(
		`/accounts/receivable/${id}/payment`,
		payload,
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function listPayables(params: {
	keyword?: string;
	status?: PayableStatus;
	page?: number;
	size?: number;
} = {}): Promise<PayableListResult> {
	const response = await apiClient.get<ApiResponse<PayableListResult>>('/accounts/payable', {
		params,
	});

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function getPayableDetail(
	supplierId: number,
	params: { startDate?: string; endDate?: string } = {},
): Promise<PayableDetailResult> {
	const response = await apiClient.get<ApiResponse<PayableDetailResult>>(
		'/accounts/payable/detail',
		{ params: { supplierId, ...params } },
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}

export async function registerPayablePayment(
	id: number,
	payload: PayablePaymentPayload,
): Promise<PayablePaymentResult> {
	const response = await apiClient.post<ApiResponse<PayablePaymentResult>>(
		`/accounts/payable/${id}/payment`,
		payload,
	);

	if (response.data.code !== 200) {
		throw new Error(response.data.message);
	}

	return response.data.data;
}
