package com.pehlione.web.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PageMeta")
public record PageMeta(
		@Schema(example = "0") int number,
		@Schema(example = "20") int size,
		@Schema(example = "120") long totalElements,
		@Schema(example = "6") int totalPages,
		@Schema(example = "true") boolean first,
		@Schema(example = "false") boolean last) {
}
