import { Button as AntButton } from 'antd';
import type { ReactNode } from 'react';
import styles from './Button.module.css';

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost' | 'link';
export type ButtonSize = 'standard' | 'compact';

export interface ButtonProps {
	variant: ButtonVariant;
	size?: ButtonSize;
	loading?: boolean;
	disabled?: boolean;
	onClick?: () => void;
	children: ReactNode;
}

const VARIANT_ANT_TYPE: Record<
	ButtonVariant,
	'primary' | 'default' | 'text' | 'link'
> = {
	primary: 'primary',
	secondary: 'default',
	danger: 'default',
	ghost: 'text',
	link: 'link',
};

export default function Button({
	variant,
	size = 'standard',
	loading = false,
	disabled = false,
	onClick,
	children,
}: ButtonProps) {
	const isDisabled = disabled || loading;

	return (
		<AntButton
			type={VARIANT_ANT_TYPE[variant]}
			className={`${styles.root} ${styles[variant]} ${styles[size]}`}
			disabled={isDisabled}
			onClick={onClick}
		>
			{loading ? '处理中…' : children}
		</AntButton>
	);
}
