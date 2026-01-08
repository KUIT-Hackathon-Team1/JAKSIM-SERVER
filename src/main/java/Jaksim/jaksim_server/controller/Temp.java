package Jaksim.jaksim_server.controller;

import Jaksim.jaksim_server.global.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Temp {
    @GetMapping
    public ResponseEntity<CommonResponse<String>> getOk() {
        return ResponseEntity.ok(CommonResponse.success("ok"));
    }
}
