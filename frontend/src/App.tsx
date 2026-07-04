import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Toast from './components/Toast/Toast';
import AppLayout from './layouts/AppLayout';
import { NAV_ITEMS } from './layouts/navConfig';
import InboundEntryPage from './pages/Inventory/InboundEntryPage';
import OutboundEntryPage from './pages/Inventory/OutboundEntryPage';
import ProductLedgerPage from './pages/Inventory/ProductLedgerPage';
import StockOverviewPage from './pages/Inventory/StockOverviewPage';
import LoginPage from './pages/Login/LoginPage';
import CustomersPage from './pages/Settings/CustomersPage';
import ProductsPage from './pages/Settings/ProductsPage';
import SuppliersPage from './pages/Settings/SuppliersPage';
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

const IMPLEMENTED_PATHS = new Set([
	'/settings/products',
	'/settings/suppliers',
	'/settings/customers',
	'/inventory/inbound',
	'/inventory/outbound',
	'/inventory/stock',
]);

export default function App() {
	return (
		<BrowserRouter>
			<Toast />
			<Routes>
				<Route path="/login" element={<LoginPage />} />
				<Route element={<ProtectedRoute />}>
					<Route element={<AppLayout />}>
						<Route path="/settings/products" element={<ProductsPage />} />
						<Route path="/settings/suppliers" element={<SuppliersPage />} />
						<Route path="/settings/customers" element={<CustomersPage />} />
						<Route path="/inventory/inbound" element={<InboundEntryPage />} />
						<Route path="/inventory/outbound" element={<OutboundEntryPage />} />
						<Route path="/inventory/stock" element={<StockOverviewPage />} />
						<Route
							path="/inventory/stock/:productId"
							element={<ProductLedgerPage />}
						/>
						{NAV_ITEMS.filter((item) => !IMPLEMENTED_PATHS.has(item.path)).map((item) => (
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
