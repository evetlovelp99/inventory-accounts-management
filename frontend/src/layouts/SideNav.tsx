import { Layout } from 'antd';
import { NavLink } from 'react-router-dom';
import { NAV_SECTIONS } from './navConfig';

const { Sider } = Layout;

const navItemStyle = (isActive: boolean) => ({
	display: 'block',
	padding: '8px 16px',
	paddingLeft: isActive ? 13 : 16,
	borderLeft: isActive ? '3px solid var(--color-amber)' : '3px solid transparent',
	color: isActive ? '#FFFFFF' : '#B0ADA8',
	fontSize: 'var(--font-size-body)',
	lineHeight: 'var(--line-height-body)',
	textDecoration: 'none',
	transition: 'background-color 0.2s',
});

export default function SideNav() {
	return (
		<Sider
			width={220}
			style={{
				background: 'var(--color-iron)',
				minHeight: '100vh',
			}}
		>
			<div
				style={{
					padding: '24px 16px',
					color: '#FFFFFF',
					fontSize: 'var(--font-size-title-2)',
					fontWeight: 'var(--font-weight-semibold)',
				}}
			>
				蜂蜡厂进销存
			</div>
			<div style={{ borderTop: '1px solid rgba(255, 255, 255, 0.12)', margin: '0 16px' }} />
			<nav style={{ padding: '8px 0 24px' }}>
				{NAV_SECTIONS.map((section, sectionIndex) => (
					<div key={section.title ?? `section-${sectionIndex}`}>
						{section.title ? (
							<div
								style={{
									padding: '16px 16px 8px',
									fontSize: 'var(--font-size-label)',
									fontWeight: 'var(--font-weight-medium)',
									lineHeight: 'var(--line-height-label)',
									color: 'var(--color-ash)',
									textTransform: 'uppercase',
									letterSpacing: '0.05em',
								}}
							>
								{section.title}
							</div>
						) : null}
						{section.items.map((item) => (
							<NavLink
								key={item.path}
								to={item.path}
								end={item.path === '/'}
								style={({ isActive }) => navItemStyle(isActive)}
								onMouseEnter={(event) => {
									event.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.08)';
								}}
								onMouseLeave={(event) => {
									event.currentTarget.style.backgroundColor = 'transparent';
								}}
							>
								{item.label}
							</NavLink>
						))}
						{section.title === '财务管理' ? (
							<div
								style={{
									borderTop: '1px solid rgba(255, 255, 255, 0.12)',
									margin: '16px 16px 0',
								}}
							/>
						) : null}
					</div>
				))}
			</nav>
		</Sider>
	);
}
