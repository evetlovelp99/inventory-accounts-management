package com.beewax.service.exchange;

import com.beewax.config.ExchangeRateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SafeGovExchangeRateProvider implements ExchangeRateProvider {

	private static final Logger log = LoggerFactory.getLogger(SafeGovExchangeRateProvider.class);

	private static final String SOURCE_CODE = "PBOC";
	private static final String QUERY_PATH = "/AppStructured/hlw/RMBQuery.do";
	private static final BigDecimal USD_UNIT_DIVISOR = new BigDecimal("100");
	private static final Pattern DATA_ROW_PATTERN = Pattern.compile(
			"<tr[^>]*>\\s*<td[^>]*>\\s*(\\d{4}-\\d{2}-\\d{2})\\s*</td>\\s*<td[^>]*>\\s*([\\d.]+)\\s*</td>",
			Pattern.CASE_INSENSITIVE);

	private final RestClient restClient;

	public SafeGovExchangeRateProvider(ExchangeRateProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
		requestFactory.setReadTimeout(properties.getReadTimeoutMs());

		this.restClient = RestClient.builder()
				.baseUrl("https://www.safe.gov.cn")
				.requestFactory(requestFactory)
				.defaultHeader("User-Agent", "Mozilla/5.0 (compatible; BeewaxERP/1.0)")
				.build();
	}

	@Override
	public Optional<ExchangeRateQuote> fetchUsdCnyRate(LocalDate date) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("startDate", date.toString());
		form.add("endDate", date.toString());
		form.add("queryYN", "true");

		try {
			String html = restClient.post()
					.uri(QUERY_PATH)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(String.class);

			if (html == null || html.isBlank()) {
				log.warn("SAFE exchange rate response was empty for date {}", date);
				return Optional.empty();
			}

			return parseUsdRate(html, date);
		} catch (RestClientException ex) {
			log.warn("Failed to fetch SAFE exchange rate for date {}", date, ex);
			return Optional.empty();
		}
	}

	@Override
	public String getSourceCode() {
		return SOURCE_CODE;
	}

	private Optional<ExchangeRateQuote> parseUsdRate(String html, LocalDate date) {
		String targetDate = date.toString();
		Matcher matcher = DATA_ROW_PATTERN.matcher(html);

		while (matcher.find()) {
			if (!targetDate.equals(matcher.group(1))) {
				continue;
			}

			BigDecimal rawUsdRate = new BigDecimal(matcher.group(2));
			BigDecimal rate = rawUsdRate.divide(USD_UNIT_DIVISOR, 4, RoundingMode.HALF_UP);
			return Optional.of(new ExchangeRateQuote(date, rate, SOURCE_CODE));
		}

		log.warn("SAFE exchange rate not found for date {}", date);
		return Optional.empty();
	}
}
