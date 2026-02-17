package com.pehlione.web.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class WebhookSignatureVerifier {

	private WebhookSignatureVerifier() {
	}

	public static boolean verifyHmacSha256Hex(String secret, String payload, String providedHex) {
		try {
			if (secret == null || secret.isBlank() || providedHex == null) {
				return false;
			}
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			String expected = toHex(digest);
			return MessageDigest.isEqual(
					expected.getBytes(StandardCharsets.UTF_8),
					providedHex.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			return false;
		}
	}

	public static String sha256Hex(String payload) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return toHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
