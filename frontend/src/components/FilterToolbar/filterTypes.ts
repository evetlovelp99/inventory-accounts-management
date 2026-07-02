export interface FilterSelectConfig {
	key: string;
	type: 'select';
	label: string;
	options: { label: string; value: string }[];
	placeholder?: string;
}

export interface FilterDateRangeConfig {
	key: string;
	type: 'dateRange';
	label: string;
}

export type FilterConfig = FilterSelectConfig | FilterDateRangeConfig;

export interface ActiveFilter {
	key: string;
	label: string;
	value: string;
}

export interface DateRangeValue {
	start?: string;
	end?: string;
}

export type FilterValue = string | DateRangeValue;
