package Jaksim.jaksim_server.domain.goal.controller;

import Jaksim.jaksim_server.domain.goal.client.GeminiClient;
import Jaksim.jaksim_server.domain.goal.dto.GetGoalRequest;
import Jaksim.jaksim_server.domain.goal.service.GoalService;
import Jaksim.jaksim_server.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/goal")
public class GoalController {
    private final GoalService goalService;
    GeminiClient geminiClient;

    @PostMapping
    public ResponseEntity<CommonResponse<String>> getGoal(@RequestHeader("X-Device-Id") String deviceId,
                                                  @RequestBody GetGoalRequest request
    ) {
        goalService.save(deviceId, request.getGoalTitle(), request.getGoalCategory(), request.getIntent());
        // AI 넘기기 로직 추가
        String result = goalService.getGoalFromAI();
        return ResponseEntity.ok(CommonResponse.success(result));
    }
}
