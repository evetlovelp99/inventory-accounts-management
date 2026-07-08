package com.beewax.service.exchange;

import com.beewax.config.ExchangeRateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Component
public class TianApiExchangeRateProvider implements ExchangeRateProvider {

	private static final Logger log = LoggerFactory.getLogger(TianApiExchangeRateProvider.class);

	private static final String SOURCE_CODE = "PBOC";
	private static final String API_URL = "https://apis.tianapi.com/fxrate/index";
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

	private final RestClient restClient;
	private final String apiKey;

	public TianApiExchangeRateProvider(ExchangeRateProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
		requestFactory.setReadTimeout(properties.getReadTimeoutMs());

		this.restClient = RestClient.builder()
				.requestFactory(requestFactory)
				.defaultHeader("User-Agent", "Mozilla/5.0 (compatible; BeewaxERP/1.0)")
				.build();
		this.apiKey = properties.getTianapiKey();
	}

	@Override
	public Optional<ExchangeRateQuote> fetchUsdCnyRate(LocalDate date) {
		if (!StringUtils.hasText(apiKey)) {
			log.warn("TianAPI exchange rate key is not configured");
			return Optional.empty();
		}

		LocalDate queryDate = date != null ? date : LocalDate.now(BUSINESS_ZONE);
		if (!queryDate.equals(LocalDate.now(BUSINESS_ZONE))) {
			log.debug("TianAPI only provides the current rate; skipping fetch for date {}", queryDate);
			return Optional.empty();
		}

		String requestUrl = UriComponentsBuilder.fromUriString(API_URL)
				.queryParam("key", apiKey)
				.queryParam("money", 1)
				.queryParam("fromcoin", "USD")
				.queryParam("tocoin", "CNY")
				.build()
				.toUriString();

		try {
			TianApiFxRateResponse response = restClient.get()
					.uri(requestUrl)
					.retrieve()
					.body(TianApiFxRateResponse.class);

			if (response == null) {
				log.warn("TianAPI exchange rate response was empty for date {}", queryDate);
				return Optional.empty();
			}

			if (response.getCode() != 200) {
				log.warn("TianAPI exchange rate request failed for date {}: code={}, msg={}",
						queryDate, response.getCode(), response.getMsg());
				return Optional.empty();
			}

			if (response.getResult() == null || !StringUtils.hasText(response.getResult().getMoney())) {
				log.warn("TianAPI exchange rate result was empty for date {}", queryDate);
				return Optional.empty();
			}

			BigDecimal rate = new BigDecimal(response.getResult().getMoney().trim())
					.setScale(4, RoundingMode.HALF_UP);
			return Optional.of(new ExchangeRateQuote(queryDate, rate, SOURCE_CODE));
		} catch (RestClientException | NumberFormatException | ArithmeticException ex) {
			log.warn("Failed to fetch TianAPI exchange rate for date {}", queryDate, ex);
			return Optional.empty();
		}
	}

	@Override
	public String getSourceCode() {
		return SOURCE_CODE;
	}
}
