export interface NavItem {
	path: string;
	label: string;
}

export interface NavSection {
	title?: string;
	items: NavItem[];
}

/** Hard-coded navigation; role filtering added in step 3.5 */
export const NAV_SECTIONS: NavSection[] = [
	{
		items: [{ path: '/', label: '首页看板' }],
	},
	{
		title: '仓库管理',
		items: [
			{ path: '/inventory/inbound', label: '录入入库' },
			{ path: '/inventory/outbound', label: '录入出库' },
			{ path: '/inventory/stock', label: '当前库存' },
		],
	},
	{
		title: '财务管理',
		items: [
			{ path: '/accounts/receivable', label: '应收账款' },
			{ path: '/accounts/payable', label: '应付账款' },
		],
	},
	{
		title: '系统设置',
		items: [
			{ path: '/settings/products', label: '产品管理' },
			{ path: '/settings/users', label: '用户管理' },
			{ path: '/settings/customers', label: '客户管理' },
			{ path: '/settings/suppliers', label: '供应商管理' },
		],
	},
];

export const NAV_ITEMS: NavItem[] = NAV_SECTIONS.flatMap((section) => section.items);

export const PAGE_TITLES: Record<string, string> = Object.fromEntries(
	NAV_ITEMS.map((item) => [item.path, item.label]),
);
