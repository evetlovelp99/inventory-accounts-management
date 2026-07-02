package com.beewax.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

	private List<T> list;
	private long total;
	private int page;
	private int size;
}
