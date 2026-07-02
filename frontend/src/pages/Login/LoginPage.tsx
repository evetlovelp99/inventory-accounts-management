import { Alert, Button, Card, Form, Input } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getLoginErrorMessage, login as loginApi } from '../../api/auth';
import { useAuthStore } from '../../store/authStore';

interface LoginFormValues {
	username: string;
	password: string;
}

export default function LoginPage() {
	const navigate = useNavigate();
	const authLogin = useAuthStore((state) => state.login);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const onFinish = async (values: LoginFormValues) => {
		setLoading(true);
		setError(null);
		try {
			const data = await loginApi(values.username, values.password);
			authLogin(data.token, data.user);
			navigate('/', { replace: true });
		} catch (err) {
			setError(getLoginErrorMessage(err));
		} finally {
			setLoading(false);
		}
	};

	return (
		<div
			style={{
				minHeight: '100vh',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				background: 'var(--color-linen)',
				padding: 'var(--space-4)',
			}}
		>
			<Card title="蜂蜡厂进销存系统" style={{ width: 400, maxWidth: '100%' }}>
				{error ? (
					<Alert
						type="error"
						message={error}
						showIcon
						style={{ marginBottom: 'var(--space-4)' }}
					/>
				) : null}
				<Form<LoginFormValues> layout="vertical" onFinish={onFinish} autoComplete="off">
					<Form.Item
						label="账号"
						name="username"
						rules={[{ required: true, message: '请输入账号' }]}
					>
						<Input placeholder="请输入登录账号" />
					</Form.Item>
					<Form.Item
						label="密码"
						name="password"
						rules={[{ required: true, message: '请输入密码' }]}
					>
						<Input.Password placeholder="请输入密码" />
					</Form.Item>
					<Button type="primary" htmlType="submit" block loading={loading}>
						登录
					</Button>
				</Form>
			</Card>
		</div>
	);
}
