package dev.skoleff.user_session_service;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class Controller {
    
    @GetMapping("/")
    public String get() {
        return "Hi";
    }

}
