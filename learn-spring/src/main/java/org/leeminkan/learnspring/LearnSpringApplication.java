package org.leeminkan.learnspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController // Marks this class as a request handler
public class LearnSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnSpringApplication.class, args);
    }

    // Maps HTTP GET requests for the path "/hello" to this method
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
