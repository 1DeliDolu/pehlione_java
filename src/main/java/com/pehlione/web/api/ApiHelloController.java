package com.pehlione.web.api;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiHelloController {

    @GetMapping("/api/v1/me")
    public String me(Principal principal) {
        return "Hello " + principal.getName();
    }
}
