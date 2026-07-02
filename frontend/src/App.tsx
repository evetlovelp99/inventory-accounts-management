import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from './pages/Login/LoginPage';

function HomePage() {
	return (
		<div style={{ padding: 'var(--space-6)' }}>
			<h1 style={{ fontSize: 'var(--font-size-title-1)', fontWeight: 'var(--font-weight-semibold)' }}>
				首页
			</h1>
		</div>
	);
}

export default function App() {
	return (
		<BrowserRouter>
			<Routes>
				<Route path="/login" element={<LoginPage />} />
				<Route path="/" element={<HomePage />} />
				<Route path="*" element={<Navigate to="/" replace />} />
			</Routes>
		</BrowserRouter>
	);
}
