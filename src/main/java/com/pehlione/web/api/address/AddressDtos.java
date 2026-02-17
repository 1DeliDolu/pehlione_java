package com.pehlione.web.api.address;

import java.time.Instant;

import com.pehlione.web.address.UserAddress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AddressDtos {

	public record CreateAddressRequest(
			@Size(max = 64) String label,
			@NotBlank @Size(max = 128) String fullName,
			@Size(max = 32) String phone,
			@NotBlank @Size(max = 255) String line1,
			@Size(max = 255) String line2,
			@NotBlank @Size(max = 128) String city,
			@Size(max = 128) String state,
			@NotBlank @Size(max = 32) String postalCode,
			@NotBlank @Pattern(regexp = "^[A-Z]{2}$") String countryCode,
			Boolean makeDefault) {
	}

	public record UpdateAddressRequest(
			@Size(max = 64) String label,
			@NotBlank @Size(max = 128) String fullName,
			@Size(max = 32) String phone,
			@NotBlank @Size(max = 255) String line1,
			@Size(max = 255) String line2,
			@NotBlank @Size(max = 128) String city,
			@Size(max = 128) String state,
			@NotBlank @Size(max = 32) String postalCode,
			@NotBlank @Pattern(regexp = "^[A-Z]{2}$") String countryCode) {
	}

	public record AddressResponse(
			@NotNull Long id,
			String label,
			String fullName,
			String phone,
			String line1,
			String line2,
			String city,
			String state,
			String postalCode,
			String countryCode,
			boolean isDefault,
			Instant createdAt,
			Instant updatedAt) {
		public static AddressResponse from(UserAddress address) {
			return new AddressResponse(
					address.getId(),
					address.getLabel(),
					address.getFullName(),
					address.getPhone(),
					address.getLine1(),
					address.getLine2(),
					address.getCity(),
					address.getState(),
					address.getPostalCode(),
					address.getCountryCode(),
					address.isDefault(),
					address.getCreatedAt(),
					address.getUpdatedAt());
		}
	}
}
