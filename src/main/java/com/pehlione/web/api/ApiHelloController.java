package com.pehlione.web.api;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Profile", description = "Authenticated profile endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class ApiHelloController {

    @GetMapping("/api/v1/me")
    public String me(Principal principal) {
        return "Hello " + principal.getName();
    }
}
