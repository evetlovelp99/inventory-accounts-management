import { Table } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import type { ExpandableConfig } from 'antd/es/table/interface';
import type { ReactNode } from 'react';
import styles from './DataTable.module.css';

const SKELETON_ROW_COUNT = 6;

export const DataTableRowClass = {
	warning: 'warningRow',
} as const;

export interface DataTableProps<T extends object> {
	columns: ColumnsType<T>;
	dataSource: T[];
	loading?: boolean;
	rowKey: string;
	onRowClick?: (row: T) => void;
	emptyText?: string;
	emptyAction?: ReactNode;
	pagination?: TablePaginationConfig | false;
	rowClassName?: (row: T) => string;
	expandable?: ExpandableConfig<T>;
}

function isActionColumn<T extends object>(column: ColumnsType<T>[number]): boolean {
	if ('key' in column && column.key === 'actions') {
		return true;
	}
	if ('dataIndex' in column && column.dataIndex === 'actions') {
		return true;
	}
	return false;
}

function getSkeletonWidth(index: number): string {
	const width = 60 + ((index * 13) % 31);
	return `${width}%`;
}

function TableEmptyState({
	emptyText,
	emptyAction,
}: {
	emptyText: string;
	emptyAction?: ReactNode;
}) {
	return (
		<div className={styles.emptyState}>
			<svg
				className={styles.emptyIcon}
				viewBox="0 0 48 48"
				fill="none"
				xmlns="http://www.w3.org/2000/svg"
				aria-hidden
			>
				<rect x="8" y="10" width="32" height="28" rx="2" stroke="currentColor" strokeWidth="2" />
				<path d="M8 18H40" stroke="currentColor" strokeWidth="2" />
				<path d="M16 26H28" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
			</svg>
			<p className={styles.emptyText}>{emptyText}</p>
			{emptyAction}
		</div>
	);
}

function TableSkeleton() {
	return (
		<div className={styles.skeletonBody}>
			{Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
				<div key={index} className={styles.skeletonRow}>
					<div className={styles.skeletonBar} style={{ width: getSkeletonWidth(index) }} />
				</div>
			))}
		</div>
	);
}

export default function DataTable<T extends object>({
	columns,
	dataSource,
	loading = false,
	rowKey,
	onRowClick,
	emptyText = '暂无数据',
	emptyAction,
	pagination,
	rowClassName,
	expandable,
}: DataTableProps<T>) {
	const enhancedColumns = columns.map((column) => {
		const nextColumn = { ...column };

		if (column.align === 'right') {
			nextColumn.className = [column.className, styles.numericHeader].filter(Boolean).join(' ');
			nextColumn.onCell = (record, index) => ({
				...(typeof column.onCell === 'function' ? column.onCell(record, index) : {}),
				className: styles.numericCell,
			});
		}

		if (isActionColumn(column)) {
			nextColumn.onCell = (record, index) => ({
				...(typeof column.onCell === 'function' ? column.onCell(record, index) : {}),
				className: styles.actionCell,
			});
		}

		return nextColumn;
	});

	const isEmpty = !loading && dataSource.length === 0;

	if (loading) {
		return (
			<div className={styles.wrapper}>
				<TableSkeleton />
			</div>
		);
	}

	if (isEmpty) {
		return (
			<div className={styles.wrapper}>
				<TableEmptyState emptyText={emptyText} emptyAction={emptyAction} />
			</div>
		);
	}

	return (
		<div className={styles.wrapper}>
			<Table<T>
				className={`${styles.table} ${styles.pagination}`}
				columns={enhancedColumns}
				dataSource={dataSource}
				rowKey={rowKey}
				pagination={pagination}
				onRow={(record) => ({
					onClick: onRowClick ? () => onRowClick(record) : undefined,
					style: { cursor: onRowClick ? 'pointer' : undefined },
				})}
				rowClassName={(record, index) =>
					[
						index % 2 === 0 ? 'rowOdd' : 'rowEven',
						rowClassName?.(record) ?? '',
					]
						.filter(Boolean)
						.join(' ')
				}
				expandable={expandable}
			/>
		</div>
	);
}
