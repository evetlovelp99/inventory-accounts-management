import { useState } from 'react';
import { useToast } from '../../hooks/useToast';
import Button from '../Button/Button';
import type {
	ActiveFilter,
	DateRangeValue,
	FilterConfig,
	FilterDateRangeConfig,
	FilterSelectConfig,
} from './filterTypes';
import styles from './FilterToolbar.module.css';

export type {
	ActiveFilter,
	FilterConfig,
	FilterDateRangeConfig,
	FilterSelectConfig,
} from './filterTypes';

export interface FilterToolbarProps {
	searchPlaceholder?: string;
	onSearch: (value: string) => void;
	filters?: FilterConfig[];
	onFilterChange: (key: string, value: string | DateRangeValue) => void;
	onExport?: () => void;
	exportLabel?: string;
	exportCount?: number;
	activeFilters: ActiveFilter[];
	onClearFilter: (key: string) => void;
	onClearAll: () => void;
}

function isDateRangeFilter(filter: FilterConfig): filter is FilterDateRangeConfig {
	return filter.type === 'dateRange';
}

function isSelectFilter(filter: FilterConfig): filter is FilterSelectConfig {
	return filter.type === 'select';
}

function getSelectValue(filterKey: string, activeFilters: ActiveFilter[]): string {
	const match = activeFilters.find((item) => item.key === filterKey);
	return match?.value ?? '';
}

function getDateRangeValue(
	filterKey: string,
	activeFilters: ActiveFilter[],
): DateRangeValue {
	const start = activeFilters.find((item) => item.key === `${filterKey}:start`);
	const end = activeFilters.find((item) => item.key === `${filterKey}:end`);
	return {
		start: start?.value,
		end: end?.value,
	};
}

export default function FilterToolbar({
	searchPlaceholder = '搜索',
	onSearch,
	filters = [],
	onFilterChange,
	onExport,
	exportLabel = '导出',
	exportCount,
	activeFilters,
	onClearFilter,
	onClearAll,
}: FilterToolbarProps) {
	const { showToast } = useToast();
	const [searchValue, setSearchValue] = useState('');

	const selectFilters = filters.filter(isSelectFilter);
	const dateRangeFilters = filters.filter(isDateRangeFilter);
	const hasSecondaryRow = dateRangeFilters.length > 0 || activeFilters.length > 0;

	const handleSearchChange = (value: string) => {
		setSearchValue(value);
		onSearch(value);
	};

	const handleClearSearch = () => {
		handleSearchChange('');
	};

	const handleExport = () => {
		onExport?.();

		if (exportCount !== undefined) {
			const hasFilters = activeFilters.length > 0 || searchValue.trim().length > 0;
			const scope = hasFilters ? '当前筛选结果' : '全部';
			showToast(`已导出 ${exportCount} 条记录（${scope}）`, 'success');
		}
	};

	return (
		<div className={styles.toolbar}>
			<div className={styles.primaryRow}>
				<div className={styles.searchWrap}>
					<span className={styles.searchIcon} aria-hidden>
						⌕
					</span>
					<input
						type="search"
						className={styles.searchInput}
						placeholder={searchPlaceholder}
						value={searchValue}
						onChange={(event) => handleSearchChange(event.target.value)}
						aria-label={searchPlaceholder}
					/>
					{searchValue ? (
						<button
							type="button"
							className={styles.clearSearchButton}
							onClick={handleClearSearch}
							aria-label="清除搜索"
						>
							✕
						</button>
					) : null}
				</div>

				{selectFilters.map((filter) => (
					<div key={filter.key} className={styles.filterControl}>
						<span className={styles.filterLabel}>{filter.label}</span>
						<select
							className={styles.select}
							value={getSelectValue(filter.key, activeFilters)}
							onChange={(event) => onFilterChange(filter.key, event.target.value)}
							aria-label={filter.label}
						>
							<option value="">{filter.placeholder ?? '全部'}</option>
							{filter.options.map((option) => (
								<option key={option.value} value={option.value}>
									{option.label}
								</option>
							))}
						</select>
					</div>
				))}

				<div className={styles.spacer} />

				{onExport ? (
					<Button variant="secondary" size="compact" onClick={handleExport}>
						{exportLabel}
					</Button>
				) : null}
			</div>

			{hasSecondaryRow ? (
				<div className={styles.secondaryRow}>
					{dateRangeFilters.map((filter) => {
						const rangeValue = getDateRangeValue(filter.key, activeFilters);
						return (
							<div key={filter.key} className={styles.filterControl}>
								<span className={styles.filterLabel}>{filter.label}：</span>
								<input
									type="date"
									className={styles.dateInput}
									value={rangeValue.start ?? ''}
									onChange={(event) =>
										onFilterChange(filter.key, {
											start: event.target.value || undefined,
											end: rangeValue.end,
										})
									}
									aria-label={`${filter.label}开始日期`}
								/>
								<span className={styles.dateSeparator}>—</span>
								<input
									type="date"
									className={styles.dateInput}
									value={rangeValue.end ?? ''}
									onChange={(event) =>
										onFilterChange(filter.key, {
											start: rangeValue.start,
											end: event.target.value || undefined,
										})
									}
									aria-label={`${filter.label}结束日期`}
								/>
							</div>
						);
					})}

					{activeFilters.length > 0 ? (
						<div className={styles.tagRow}>
							{activeFilters.map((filter) => (
								<span key={filter.key} className={styles.tag}>
									{filter.label}：{filter.displayValue ?? filter.value}
									<button
										type="button"
										className={styles.tagClose}
										onClick={() => onClearFilter(filter.key)}
										aria-label={`清除${filter.label}`}
									>
										✕
									</button>
								</span>
							))}
							<button type="button" className={styles.clearAllButton} onClick={onClearAll}>
								清除全部
							</button>
						</div>
					) : null}
				</div>
			) : null}
		</div>
	);
}
