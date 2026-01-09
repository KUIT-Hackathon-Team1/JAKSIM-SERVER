package Jaksim.jaksim_server.domain.badge.service;

import Jaksim.jaksim_server.domain.badge.dto.*;
import Jaksim.jaksim_server.domain.progress.model.ChallengeRun;
import Jaksim.jaksim_server.domain.progress.model.enums.RunStatus;
import Jaksim.jaksim_server.domain.progress.model.enums.TierStatus;
import Jaksim.jaksim_server.domain.progress.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private static final int TOTAL_DAYS = 3;

    private final ChallengeRunRepository runRepository;

    public List<BadgeItemResponse> getBadges(Long userId) {
        return runRepository.findAllByGoal_User_IdOrderByStartDateDesc(userId)
                .stream()
                .map(this::toItem)
                .toList();
    }

    public BadgeSummaryResponse getSummary(Long userId) {
        List<ChallengeRun> runs = runRepository.findAllByGoal_User_IdOrderByStartDateDesc(userId);

        int total = runs.size();
        int inProgress = (int) runs.stream().filter(r -> r.getRunStatus() == RunStatus.IN_PROGRESS).count();
        int ended = total - inProgress;

        int gold = (int) runs.stream()
                .filter(r -> r.getRunStatus() == RunStatus.ENDED)
                .filter(r -> r.getTierStatus() == TierStatus.GOLD)
                .count();

        int bronze = (int) runs.stream()
                .filter(r -> r.getRunStatus() == RunStatus.ENDED)
                .filter(r -> r.getTierStatus() == TierStatus.BRONZE)
                .count();

        int fail = (int) runs.stream()
                .filter(r -> r.getRunStatus() == RunStatus.ENDED)
                .filter(r -> r.getTierStatus() == TierStatus.FAIL)
                .count();

        double goldRate = (ended == 0) ? 0.0 : ((double) gold / (double) ended);

        return new BadgeSummaryResponse(
                total,
                inProgress,
                ended,
                gold,
                bronze,
                fail,
                goldRate
        );
    }

    private BadgeItemResponse toItem(ChallengeRun run) {
        // 방어 로직: 혹시 ended인데 tierStatus null이면 FAIL로 취급(데이터 꼬임 방지)
        TierStatus tier = run.getTierStatus();
        if (run.getRunStatus() == RunStatus.ENDED && tier == null) {
            tier = TierStatus.FAIL;
        }

        List<BadgeBorderStatus> border = buildBorder(run, tier);

        return new BadgeItemResponse(
                run.getId(),
                run.getGoal().getId(),
                run.getGoal().getTitle(),
                run.getGoal().getCategory(),
                run.getGoal().getCategory().getIconKey(),
                run.getRunStatus(),
                tier, // IN_PROGRESS면 null 유지
                run.getStartDate(),
                run.getExpectedEndDate(),
                run.getEndedAt(),
                border
        );
    }

    private List<BadgeBorderStatus> buildBorder(ChallengeRun run, TierStatus tier) {
        // 기본: 3칸 모두 EMPTY
        List<BadgeBorderStatus> border = new ArrayList<>(List.of(
                BadgeBorderStatus.EMPTY,
                BadgeBorderStatus.EMPTY,
                BadgeBorderStatus.EMPTY
        ));

        // 1) 진행 중: "지나간 날 = DONE", "오늘 = PARTIAL", "미래 = EMPTY"
        if (run.getRunStatus() == RunStatus.IN_PROGRESS) {
            int elapsed = daysInclusive(run.getStartDate(), LocalDate.now()); // 1..∞
            int doneDays = clamp(elapsed - 1, 0, TOTAL_DAYS); // 어제까지 DONE

            for (int i = 0; i < doneDays; i++) border.set(i, BadgeBorderStatus.DONE);

            if (doneDays < TOTAL_DAYS) border.set(doneDays, BadgeBorderStatus.PARTIAL); // 오늘
            return border;
        }

        // 2) 종료: tier 기준으로 링 결정
        if (tier == TierStatus.GOLD) {
            for (int i = 0; i < TOTAL_DAYS; i++) border.set(i, BadgeBorderStatus.DONE);
            return border;
        }

        if (tier == TierStatus.BRONZE) {
            border.set(0, BadgeBorderStatus.DONE);
            border.set(1, BadgeBorderStatus.DONE);
            border.set(2, BadgeBorderStatus.PARTIAL);
            return border;
        }

        // 3) FAIL: 종료된 날짜 기준으로 "실패한 날"에 FAIL을 찍고, 그 뒤는 EMPTY
        LocalDate endedDate = (run.getEndedAt() != null)
                ? run.getEndedAt().toLocalDate()
                : (run.getExpectedEndDate() != null ? run.getExpectedEndDate() : run.getStartDate());

        int reached = clamp(daysInclusive(run.getStartDate(), endedDate), 1, TOTAL_DAYS);
        // reached-1일까지는 DONE(거기까지는 살아있었다는 가정), reached번째 날 FAIL
        for (int i = 0; i < reached - 1; i++) border.set(i, BadgeBorderStatus.DONE);
        border.set(reached - 1, BadgeBorderStatus.FAIL);

        // 나머지는 그대로 EMPTY -> "3개 채우기 전에 죽으면 빈 공간 남음" 충족
        return border;
    }

    private int daysInclusive(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
