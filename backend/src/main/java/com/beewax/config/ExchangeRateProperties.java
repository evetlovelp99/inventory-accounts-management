package com.beewax.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.exchange-rate")
@Getter
@Setter
public class ExchangeRateProperties {

	private int connectTimeoutMs = 5000;

	private int readTimeoutMs = 10000;

	private String tianapiKey = "";
}
