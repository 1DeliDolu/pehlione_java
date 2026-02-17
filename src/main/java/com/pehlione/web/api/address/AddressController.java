package com.pehlione.web.api.address;

import static com.pehlione.web.api.address.AddressDtos.AddressResponse;
import static com.pehlione.web.api.address.AddressDtos.CreateAddressRequest;
import static com.pehlione.web.api.address.AddressDtos.UpdateAddressRequest;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.address.AddressService;
import com.pehlione.web.address.UserAddress;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

	private final AddressService service;

	public AddressController(AddressService service) {
		this.service = service;
	}

	@GetMapping
	public List<AddressResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return service.list(jwt.getSubject()).stream().map(AddressResponse::from).toList();
	}

	@PostMapping
	public AddressResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateAddressRequest req) {
		UserAddress address = new UserAddress();
		address.setLabel(trimOrNull(req.label()));
		address.setFullName(req.fullName().trim());
		address.setPhone(trimOrNull(req.phone()));
		address.setLine1(req.line1().trim());
		address.setLine2(trimOrNull(req.line2()));
		address.setCity(req.city().trim());
		address.setState(trimOrNull(req.state()));
		address.setPostalCode(req.postalCode().trim());
		address.setCountryCode(req.countryCode().trim());
		boolean makeDefault = req.makeDefault() != null && req.makeDefault();
		return AddressResponse.from(service.create(jwt.getSubject(), address, makeDefault));
	}

	@PutMapping("/{id}")
	public AddressResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("id") Long id,
			@Valid @RequestBody UpdateAddressRequest req) {
		return AddressResponse.from(service.update(jwt.getSubject(), id, address -> {
			address.setLabel(trimOrNull(req.label()));
			address.setFullName(req.fullName().trim());
			address.setPhone(trimOrNull(req.phone()));
			address.setLine1(req.line1().trim());
			address.setLine2(trimOrNull(req.line2()));
			address.setCity(req.city().trim());
			address.setState(trimOrNull(req.state()));
			address.setPostalCode(req.postalCode().trim());
			address.setCountryCode(req.countryCode().trim());
		}));
	}

	@PostMapping("/{id}/default")
	public void setDefault(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
		service.setDefault(jwt.getSubject(), id);
	}

	@DeleteMapping("/{id}")
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
		service.delete(jwt.getSubject(), id);
	}

	private String trimOrNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}
}
