package Jaksim.jaksim_server.domain.user.controller;

import Jaksim.jaksim_server.domain.user.dto.UserResponse;
import Jaksim.jaksim_server.global.response.CommonResponse;
import Jaksim.jaksim_server.domain.user.service.UserStatService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserStatService userService;

    @Operation(
            summary = "사용자 달성 정보 가져오기",
            description = "사용자의 완료 루프, 연속 달성 가져오기, 달성률 가져오기"
    )
    @GetMapping("/user")
    public ResponseEntity<CommonResponse<UserResponse>> getUserStats() {
        UserResponse response = userService.getUserStats();
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
