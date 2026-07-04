import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Toast from './components/Toast/Toast';
import AppLayout from './layouts/AppLayout';
import { NAV_ITEMS } from './layouts/navConfig';
import LoginPage from './pages/Login/LoginPage';
import ProductsPage from './pages/Settings/ProductsPage';
import ProtectedRoute from './routes/ProtectedRoute';

function PlaceholderPage({ title }: { title: string }) {
	return (
		<h1
			style={{
				margin: 0,
				fontSize: 'var(--font-size-title-1)',
				fontWeight: 'var(--font-weight-semibold)',
				color: 'var(--color-ink)',
			}}
		>
			{title}
		</h1>
	);
}

export default function App() {
	return (
		<BrowserRouter>
			<Toast />
			<Routes>
				<Route path="/login" element={<LoginPage />} />
				<Route element={<ProtectedRoute />}>
					<Route element={<AppLayout />}>
						<Route path="/settings/products" element={<ProductsPage />} />
						{NAV_ITEMS.filter((item) => item.path !== '/settings/products').map((item) => (
							<Route
								key={item.path}
								path={item.path}
								element={<PlaceholderPage title={item.label} />}
							/>
						))}
					</Route>
				</Route>
				<Route path="*" element={<Navigate to="/" replace />} />
			</Routes>
		</BrowserRouter>
	);
}
