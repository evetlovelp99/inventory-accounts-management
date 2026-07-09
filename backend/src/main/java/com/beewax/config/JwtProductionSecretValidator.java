package com.beewax.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile("prod")
public class JwtProductionSecretValidator {

	public JwtProductionSecretValidator(JwtConfig jwtConfig) {
		if (!StringUtils.hasText(jwtConfig.getSecret())) {
			throw new IllegalStateException(
					"JWT secret must be configured via JWT_SECRET environment variable in production");
		}
	}
}
