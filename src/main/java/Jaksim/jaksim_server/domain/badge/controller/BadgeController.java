package Jaksim.jaksim_server.domain.badge.controller;

import Jaksim.jaksim_server.domain.badge.dto.BadgeItemResponse;
import Jaksim.jaksim_server.domain.badge.dto.BadgeSummaryResponse;
import Jaksim.jaksim_server.domain.badge.service.BadgeService;
import Jaksim.jaksim_server.domain.user.model.User;
import Jaksim.jaksim_server.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/badges")
public class BadgeController {

    private final UserService userService;
    private final BadgeService badgeService;

    private Long userIdFrom(String deviceId) {
        return userService.getOrCreateByDeviceId(deviceId).getId();
    }

    @GetMapping
    public List<BadgeItemResponse> list(
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestParam(defaultValue = "true") boolean includeInProgress
    ) {
        return badgeService.getBadges(userIdFrom(deviceId), includeInProgress);
    }

    @GetMapping("/summary")
    public BadgeSummaryResponse summary(
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        return badgeService.getSummary(userIdFrom(deviceId));
    }
}
