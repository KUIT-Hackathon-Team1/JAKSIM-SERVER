package Jaksim.jaksim_server.domain.progress.controller;

import Jaksim.jaksim_server.domain.progress.dto.*;
import Jaksim.jaksim_server.domain.progress.service.ProgressService;
import Jaksim.jaksim_server.domain.user.model.User;
import Jaksim.jaksim_server.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final UserService userService;
    private final ProgressService progressService;

    private Long userFrom(String deviceId) {
        return userService.getOrCreateByDeviceId(deviceId).getId();
    }

    @PostMapping("/runs")
    public RunDetailResponse startRun(
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestBody StartRunRequest request
    ) {
        return progressService.startRun(userFrom(deviceId), request);
    }

    @GetMapping("/runs/{runId}")
    public RunDetailResponse getRunDetail(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long runId
    ) {
        return progressService.getRunDetail(userFrom(deviceId), runId);
    }

    @PatchMapping("/runs/{runId}/days/{dayIndex}")
    public RunDetailResponse updateDay(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long runId,
            @PathVariable int dayIndex,
            @RequestBody UpdateDayRequest request
    ) {
        return progressService.updateDay(userFrom(deviceId), runId, dayIndex, request);
    }

    @PostMapping("/runs/{runId}/give-up")
    public RunDetailResponse giveUp(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long runId
    ) {
        return progressService.giveUp(userFrom(deviceId), runId);
    }
}
