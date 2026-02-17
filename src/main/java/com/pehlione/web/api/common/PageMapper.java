package com.pehlione.web.api.common;

import org.springframework.data.domain.Page;

public final class PageMapper {

	private PageMapper() {
	}

	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				new PageMeta(
						page.getNumber(),
						page.getSize(),
						page.getTotalElements(),
						page.getTotalPages(),
						page.isFirst(),
						page.isLast()));
	}
}
