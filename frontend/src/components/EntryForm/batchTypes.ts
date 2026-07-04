export interface InboundBatch {
	inboundId: number;
	inboundDate: string;
	unitPrice: number;
	remainingQty: number;
	unit: string;
	supplierName: string;
}

export interface BatchRowValue {
	batchId: number;
	qty: number;
}
