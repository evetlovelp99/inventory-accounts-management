import { Button, Layout, Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, type UserRole } from '../store/authStore';

const { Header } = Layout;

const ROLE_LABELS: Record<UserRole, string> = {
	OWNER: '老板',
	FINANCE: '财务',
	WAREHOUSE: '仓管',
	SUPERVISOR: '主管',
};

interface TopBarProps {
	pageTitle: string;
}

export default function TopBar({ pageTitle }: TopBarProps) {
	const navigate = useNavigate();
	const user = useAuthStore((state) => state.user);
	const logout = useAuthStore((state) => state.logout);

	const handleLogout = () => {
		logout();
		navigate('/login', { replace: true });
	};

	return (
		<Header
			style={{
				height: 56,
				lineHeight: '56px',
				padding: '0 var(--content-padding-x)',
				background: 'var(--color-paper)',
				borderBottom: '1px solid var(--color-dust)',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'space-between',
			}}
		>
			<span
				style={{
					fontSize: 'var(--font-size-title-1)',
					fontWeight: 'var(--font-weight-semibold)',
					color: 'var(--color-ink)',
				}}
			>
				{pageTitle}
			</span>
			<div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
				<span style={{ color: 'var(--color-ink)' }}>{user?.name}</span>
				{user ? <Tag>{ROLE_LABELS[user.role]}</Tag> : null}
				<Button type="link" onClick={handleLogout}>
					退出
				</Button>
			</div>
		</Header>
	);
}
