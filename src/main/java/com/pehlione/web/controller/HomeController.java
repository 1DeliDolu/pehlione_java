package com.pehlione.web.controller;

import java.time.OffsetDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<ApiStatus> home() {
        return ResponseEntity.ok(new ApiStatus("pehlione", "UP", OffsetDateTime.now()));
    }

    @RequestMapping("/api/v1")
    public ResponseEntity<ApiRoot> apiRoot() {
        return ResponseEntity.ok(new ApiRoot("API root", "v1"));
    }

    public record ApiStatus(String app, String status, OffsetDateTime time) {
    }

    public record ApiRoot(String message, String version) {
    }
}
