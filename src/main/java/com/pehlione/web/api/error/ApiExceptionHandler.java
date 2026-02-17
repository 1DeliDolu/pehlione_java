package com.pehlione.web.api.error;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> handleRateLimited(RateLimitExceededException ex, HttpServletRequest req) {
        ProblemDetail pd = base(ex.getStatus(), ex.getMessage(), req);
        pd.setType(ProblemTypes.RATE_LIMITED);
        pd.setTitle("Too Many Requests");
        pd.setProperty("code", ex.getCode().name());
        pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        return response(pd);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest req) {
        ProblemDetail pd = base(ex.getStatus(), ex.getMessage(), req);
        pd.setType(typeFor(ex.getStatus(), ex.getCode()));
        pd.setTitle(titleFor(ex.getStatus()));
        pd.setProperty("code", ex.getCode().name());
        return response(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.BAD_REQUEST, "Validation failed", req);
        pd.setType(ProblemTypes.VALIDATION);
        pd.setTitle("Validation error");
        pd.setProperty("code", ApiErrorCode.VALIDATION_FAILED.name());

        List<Map<String, Object>> violations = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("field", fe.getField());
            v.put("message", fe.getDefaultMessage());
            v.put("rejectedValue", fe.getRejectedValue());
            violations.add(v);
        }
        pd.setProperty("violations", violations);
        return response(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolations(ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.BAD_REQUEST, "Validation failed", req);
        pd.setType(ProblemTypes.VALIDATION);
        pd.setTitle("Validation error");
        pd.setProperty("code", ApiErrorCode.VALIDATION_FAILED.name());

        List<Map<String, Object>> violations = new ArrayList<>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("path", String.valueOf(cv.getPropertyPath()));
            v.put("message", cv.getMessage());
            v.put("invalidValue", cv.getInvalidValue());
            violations.add(v);
        }
        pd.setProperty("violations", violations);
        return response(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.BAD_REQUEST, "Malformed JSON request body", req);
        pd.setType(ProblemTypes.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setProperty("code", ApiErrorCode.VALIDATION_FAILED.name());
        return response(pd);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.UNAUTHORIZED, "Authentication required", req);
        pd.setType(ProblemTypes.UNAUTHORIZED);
        pd.setTitle("Unauthorized");
        pd.setProperty("code", ApiErrorCode.UNAUTHORIZED.name());
        return response(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.FORBIDDEN, "Access denied", req);
        pd.setType(ProblemTypes.FORBIDDEN);
        pd.setTitle("Forbidden");
        pd.setProperty("code", ApiErrorCode.FORBIDDEN.name());
        return response(pd);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.CONFLICT, "Data integrity violation", req);
        pd.setType(ProblemTypes.CONFLICT);
        pd.setTitle("Conflict");
        pd.setProperty("code", ApiErrorCode.CONFLICT.name());
        return response(pd);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", req);
        pd.setType(ProblemTypes.BAD_REQUEST);
        pd.setTitle("Method Not Allowed");
        pd.setProperty("code", ApiErrorCode.VALIDATION_FAILED.name());
        return response(pd);
    }

    @ExceptionHandler(ErrorResponseException.class)
    ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException ex, HttpServletRequest req) {
        HttpStatusCode status = ex.getStatusCode();
        String detail = (ex.getBody() != null && ex.getBody().getDetail() != null)
                ? ex.getBody().getDetail()
                : "Request failed";
        ProblemDetail pd = base(status, detail, req);
        pd.setType(typeFor(status, statusToCode(status)));
        pd.setTitle(titleFor(status));
        pd.setProperty("code", statusToCode(status).name());
        return response(pd);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnknown(Exception ex, HttpServletRequest req) {
        ProblemDetail pd = base(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req);
        pd.setType(ProblemTypes.INTERNAL);
        pd.setTitle("Internal Server Error");
        pd.setProperty("code", ApiErrorCode.INTERNAL_ERROR.name());
        return response(pd);
    }

    private ProblemDetail base(HttpStatusCode status, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(titleFor(status));
        pd.setInstance(toUri(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        String requestId = requestId(req);
        if (requestId != null) {
            pd.setProperty("requestId", requestId);
        }
        return pd;
    }

    private URI typeFor(HttpStatusCode status, ApiErrorCode code) {
        if (code == ApiErrorCode.VALIDATION || code == ApiErrorCode.VALIDATION_FAILED) {
            return ProblemTypes.VALIDATION;
        }
        if (code == ApiErrorCode.UNAUTHORIZED) {
            return ProblemTypes.UNAUTHORIZED;
        }
        if (code == ApiErrorCode.FORBIDDEN) {
            return ProblemTypes.FORBIDDEN;
        }
        if (code == ApiErrorCode.NOT_FOUND) {
            return ProblemTypes.NOT_FOUND;
        }
        if (code == ApiErrorCode.CONFLICT) {
            return ProblemTypes.CONFLICT;
        }
        if (code == ApiErrorCode.RATE_LIMITED) {
            return ProblemTypes.RATE_LIMITED;
        }
        int s = status.value();
        if (s == 400) {
            return ProblemTypes.BAD_REQUEST;
        }
        if (s == 401) {
            return ProblemTypes.UNAUTHORIZED;
        }
        if (s == 403) {
            return ProblemTypes.FORBIDDEN;
        }
        if (s == 404) {
            return ProblemTypes.NOT_FOUND;
        }
        if (s == 409) {
            return ProblemTypes.CONFLICT;
        }
        if (s == 429) {
            return ProblemTypes.RATE_LIMITED;
        }
        return ProblemTypes.INTERNAL;
    }

    private ApiErrorCode statusToCode(HttpStatusCode status) {
        int s = status.value();
        if (s == 400) {
            return ApiErrorCode.VALIDATION_FAILED;
        }
        if (s == 401) {
            return ApiErrorCode.UNAUTHORIZED;
        }
        if (s == 403) {
            return ApiErrorCode.FORBIDDEN;
        }
        if (s == 404) {
            return ApiErrorCode.NOT_FOUND;
        }
        if (s == 409) {
            return ApiErrorCode.CONFLICT;
        }
        if (s == 429) {
            return ApiErrorCode.RATE_LIMITED;
        }
        return ApiErrorCode.INTERNAL_ERROR;
    }

    private String titleFor(HttpStatusCode status) {
        HttpStatus hs = HttpStatus.resolve(status.value());
        return (hs == null) ? "Error" : hs.getReasonPhrase();
    }

    private String requestId(HttpServletRequest req) {
        String header = req.getHeader("X-Request-Id");
        if (header != null && !header.isBlank()) {
            return header;
        }
        String mdc = MDC.get("requestId");
        return (mdc == null || mdc.isBlank()) ? null : mdc;
    }

    private URI toUri(String path) {
        try {
            return URI.create(path);
        } catch (Exception ex) {
            return URI.create("/");
        }
    }

    private ResponseEntity<ProblemDetail> response(ProblemDetail pd) {
        return ResponseEntity.status(pd.getStatus()).contentType(PROBLEM_JSON).body(pd);
    }
}
