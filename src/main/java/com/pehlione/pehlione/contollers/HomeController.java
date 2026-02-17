package com.pehlione.pehlione.contollers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HomeController {
    @GetMapping("/")
    public String getMethodName() {
        return "hello Pehlione";
    }
    
    
}
