package com.beewax.dto.response;

import com.beewax.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductResponse {

	private Long id;
	private String name;
	private String spec;
	private String unit;
	private String status;

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getSpec(),
				product.getUnit(),
				product.getStatus().name());
	}
}
