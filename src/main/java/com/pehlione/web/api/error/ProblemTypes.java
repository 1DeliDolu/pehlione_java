package com.pehlione.web.api.error;

import java.net.URI;

public final class ProblemTypes {

	private ProblemTypes() {
	}

	public static final URI VALIDATION = URI.create("urn:problem:validation");
	public static final URI UNAUTHORIZED = URI.create("urn:problem:unauthorized");
	public static final URI FORBIDDEN = URI.create("urn:problem:forbidden");
	public static final URI NOT_FOUND = URI.create("urn:problem:not-found");
	public static final URI CONFLICT = URI.create("urn:problem:conflict");
	public static final URI RATE_LIMITED = URI.create("urn:problem:rate-limited");
	public static final URI BAD_REQUEST = URI.create("urn:problem:bad-request");
	public static final URI INTERNAL = URI.create("urn:problem:internal");
}
