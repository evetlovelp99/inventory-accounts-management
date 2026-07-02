import { Layout } from 'antd';
import { Outlet, useLocation } from 'react-router-dom';
import AlertBar from '../components/AlertBar/AlertBar';
import SideNav from './SideNav';
import TopBar from './TopBar';
import { PAGE_TITLES } from './navConfig';

const { Content } = Layout;

export default function AppLayout() {
	const { pathname } = useLocation();
	const pageTitle = PAGE_TITLES[pathname] ?? '首页看板';

	return (
		<Layout style={{ minHeight: '100vh' }}>
			<SideNav />
			<Layout>
				<TopBar pageTitle={pageTitle} />
				<AlertBar />
				<Content
					style={{
						padding: 'var(--content-padding-top) var(--content-padding-x)',
						background: 'var(--color-linen)',
						minHeight: 'calc(100vh - 56px)',
					}}
				>
					<div style={{ maxWidth: 1200 }}>
						<Outlet />
					</div>
				</Content>
			</Layout>
		</Layout>
	);
}
