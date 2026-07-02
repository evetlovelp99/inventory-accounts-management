import type { ThemeConfig } from 'antd';

/** Color palette from design/theme.md */
export const colors = {
	linen: '#F5F3EF',
	paper: '#FDFCFA',
	amber: '#C8860A',
	amberLight: '#FDF3DC',
	iron: '#3D3D3D',
	ink: '#1A1A1A',
	ash: '#6B6B6B',
	dust: '#E5E2DC',
	grove: '#2D6A4F',
	groveLight: '#D8EDDF',
	brick: '#B94040',
	brickLight: '#FAEAEA',
	clay: '#A05C1A',
	clayLight: '#FFF0DC',
	clayDark: '#8B3A10',
} as const;

/** Ant Design token overrides mapped from design/theme.md */
export const appTheme: ThemeConfig = {
	token: {
		colorPrimary: colors.amber,
		colorPrimaryBg: colors.amberLight,
		colorBgBase: colors.linen,
		colorBgContainer: colors.paper,
		colorBgLayout: colors.linen,
		colorText: colors.ink,
		colorTextSecondary: colors.ash,
		colorBorder: colors.dust,
		colorSplit: colors.dust,
		colorSuccess: colors.grove,
		colorSuccessBg: colors.groveLight,
		colorError: colors.brick,
		colorErrorBg: colors.brickLight,
		colorWarning: colors.clay,
		colorWarningBg: colors.clayLight,
		borderRadius: 4,
		borderRadiusLG: 6,
		borderRadiusSM: 2,
		fontSize: 14,
		fontSizeLG: 16,
		fontSizeSM: 12,
		fontSizeHeading1: 22,
		fontSizeHeading2: 16,
		lineHeight: 22 / 14,
		controlHeight: 40,
		fontFamily: '"PingFang SC", "Microsoft YaHei", sans-serif',
		fontFamilyCode: '"IBM Plex Mono", ui-monospace, monospace',
	},
	components: {
		Layout: {
			siderBg: colors.iron,
		},
		Table: {
			headerBg: colors.iron,
			headerColor: colors.paper,
		},
		Button: {
			controlHeight: 40,
			borderRadius: 4,
		},
		Input: {
			controlHeight: 40,
			borderRadius: 4,
		},
		Select: {
			controlHeight: 40,
			borderRadius: 4,
		},
		Modal: {
			borderRadiusLG: 6,
		},
		Tag: {
			borderRadiusSM: 2,
		},
	},
};
