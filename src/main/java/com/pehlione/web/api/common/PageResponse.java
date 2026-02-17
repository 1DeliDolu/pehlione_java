package com.pehlione.web.api.common;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PageResponse")
public record PageResponse<T>(
		@Schema(description = "Listed items") List<T> items,
		@Schema(description = "Paging metadata") PageMeta page) {
}
