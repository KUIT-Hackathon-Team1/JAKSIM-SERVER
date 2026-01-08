package Jaksim.jaksim_server.domain.badge.service;

import Jaksim.jaksim_server.domain.badge.dto.BadgeItemResponse;
import Jaksim.jaksim_server.domain.badge.dto.BadgeSummaryResponse;
import Jaksim.jaksim_server.domain.progress.model.ChallengeRun;
import Jaksim.jaksim_server.domain.progress.model.enums.RunStatus;
import Jaksim.jaksim_server.domain.progress.model.enums.TierStatus;
import Jaksim.jaksim_server.domain.progress.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final ChallengeRunRepository runRepository;

    public List<BadgeItemResponse> getBadges(Long userId, boolean includeInProgress) {
        List<ChallengeRun> runs = runRepository.findAllByGoal_User_IdOrderByStartDateDesc(userId);

        return runs.stream()
                .filter(r -> includeInProgress || r.getRunStatus() == RunStatus.ENDED)
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

        return new BadgeItemResponse(
                run.getId(),
                run.getGoal().getId(),
                run.getGoal().getTitle(),
                run.getGoal().getCategory().getIconKey(),
                run.getRunStatus(),
                tier, // IN_PROGRESS면 null 유지
                run.getStartDate(),
                run.getExpectedEndDate(),
                run.getEndedAt()
        );
    }
}
