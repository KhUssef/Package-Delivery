package ai.proj.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    // GET request at /
    @GetMapping("/")
    public String home() {
        return "Hello from Spring Boot!";
    }

    // GET request at /hello
    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

    // POST request at /echo
    @PostMapping("/echo")
    public String echo(@RequestBody String message) {
        return "You said: " + message;
    }
}
