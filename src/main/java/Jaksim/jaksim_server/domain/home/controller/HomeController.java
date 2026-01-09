package Jaksim.jaksim_server.domain.home.controller;

import Jaksim.jaksim_server.domain.badge.dto.BadgeItemResponse;
import Jaksim.jaksim_server.domain.badge.dto.BadgeSummaryResponse;
import Jaksim.jaksim_server.domain.badge.service.BadgeService;
import Jaksim.jaksim_server.domain.home.dto.HomeResponse;
import Jaksim.jaksim_server.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final UserService userService;
    private final BadgeService badgeService;

    private Long userIdFrom(String deviceId) {
        return userService.getOrCreateByDeviceId(deviceId).getId(); // PK getter 맞게
    }

    @GetMapping
    public HomeResponse home(@RequestHeader("X-Device-Id") String deviceId) {
        Long userId = userIdFrom(deviceId);

        BadgeSummaryResponse summary = badgeService.getSummary(userId);
        List<BadgeItemResponse> badges = badgeService.getBadges(userId); // 전체 반환 고정 추천

        boolean hasInProgress = badges.stream().anyMatch(b -> b.runStatus().name().equals("IN_PROGRESS"));
        String newGoalIconKey = hasInProgress ? null : "star";

        return new HomeResponse(summary, hasInProgress, newGoalIconKey, badges);
    }
}
