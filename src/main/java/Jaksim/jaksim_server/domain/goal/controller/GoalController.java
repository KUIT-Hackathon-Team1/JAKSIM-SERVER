package Jaksim.jaksim_server.domain.goal.controller;

import Jaksim.jaksim_server.domain.goal.dto.CreateGoalRequest;
import Jaksim.jaksim_server.domain.goal.dto.GetGoalRequest;
import Jaksim.jaksim_server.domain.goal.dto.SuggestGoalRequest;
import Jaksim.jaksim_server.domain.goal.service.GoalService;
import Jaksim.jaksim_server.domain.user.service.UserService;
import Jaksim.jaksim_server.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/goal")
public class GoalController {
    private final UserService userService;
    private final GoalService goalService;

    private Long userFrom(String deviceId) {
        return userService.getOrCreateByDeviceId(deviceId).getId();
    }

    @PostMapping("/ai")
    public ResponseEntity<CommonResponse<String[]>> getGoal(@RequestHeader("X-Device-Id") String deviceId,
                                                  @RequestBody SuggestGoalRequest request
    ) {
        Long userId = userFrom(deviceId);
        String[] result = goalService.suggestGoals(userId,request);
        return ResponseEntity.ok(CommonResponse.success(result));
    }

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<Long>> save(
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestBody CreateGoalRequest request
    ) {
        Long goalId = goalService.save(deviceId, request);
        return ResponseEntity.ok(CommonResponse.success(goalId));
    }

    @PostMapping("/{baseGoalId}/keep")
    public CommonResponse<Long> keep(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long baseGoalId
    ) {
        Long goalId = goalService.keep(deviceId, baseGoalId);
        return CommonResponse.success(goalId);
    }
}
