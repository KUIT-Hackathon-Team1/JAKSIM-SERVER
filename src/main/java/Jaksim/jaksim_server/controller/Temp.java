package Jaksim.jaksim_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Temp {
    @GetMapping
    public String getOk() {
        return "ok";
    }
}
