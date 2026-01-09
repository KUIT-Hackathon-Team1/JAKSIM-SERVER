package Jaksim.jaksim_server.domain.badge.service;

import Jaksim.jaksim_server.domain.badge.dto.*;
import Jaksim.jaksim_server.domain.progress.model.ChallengeDay;
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
    private final ChallengeDayRepository dayRepository;

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

        List<ChallengeDay> days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(run.getId());
        List<BadgeBorderStatus> border = buildBorder(days, run.getTargetDays()); // 보통 3


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

    private List<BadgeBorderStatus> buildBorder(List<ChallengeDay> days, int targetDays) {
        List<BadgeBorderStatus> border = new ArrayList<>();
        for (int i = 1; i <= targetDays; i++) border.add(BadgeBorderStatus.EMPTY);

        for (ChallengeDay d : days) {
            int idx = d.getDayIndex() - 1;
            if (idx < 0 || idx >= targetDays) continue;

            if (!d.isFinalized()) {
                border.set(idx, BadgeBorderStatus.EMPTY);
                continue;
            }

            switch (d.getDayResult()) {
                case DONE -> border.set(idx, BadgeBorderStatus.DONE);
                case PARTIAL -> border.set(idx, BadgeBorderStatus.PARTIAL);
                case FAIL -> border.set(idx, BadgeBorderStatus.FAIL);
                default -> border.set(idx, BadgeBorderStatus.EMPTY); // NOT_SET 방어
            }
        }
        return border;
    }

}
