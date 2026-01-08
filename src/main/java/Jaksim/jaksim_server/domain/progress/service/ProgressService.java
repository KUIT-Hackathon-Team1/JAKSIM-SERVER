package Jaksim.jaksim_server.domain.progress.service;

import Jaksim.jaksim_server.domain.goal.model.Goal;
import Jaksim.jaksim_server.domain.goal.repository.GoalRepository;
import Jaksim.jaksim_server.domain.progress.dto.*;
import Jaksim.jaksim_server.domain.progress.model.*;
import Jaksim.jaksim_server.domain.progress.model.enums.*;
import Jaksim.jaksim_server.domain.progress.repository.*;
import Jaksim.jaksim_server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final GoalRepository goalRepository;
    private final ChallengeRunRepository runRepository;
    private final ChallengeDayRepository dayRepository;

    @Transactional
    public RunDetailResponse startRun(Long userId, StartRunRequest req) {
        // 1) Goal 소유 검증 포함해서 조회 (메서드명은 GoalRepository에 맞춰 조정)
        Goal goal = goalRepository.findByIdAndUserId(req.goalId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 목표가 없습니다."));

        // 2) 목표당 진행중 run 1개 제한
        runRepository.findTopByGoal_IdAndRunStatusOrderByStartDateDesc(goal.getId(), RunStatus.IN_PROGRESS)
                .ifPresent(r -> { throw new IllegalStateException("이미 진행 중인 3일 목표가 있습니다."); });

        LocalDate startDate = (req.startDate() != null) ? req.startDate() : LocalDate.now();

        // 3) Run 생성 (goal 엔티티 연결)
        ChallengeRun run = runRepository.save(ChallengeRun.start(goal, startDate));

        // 4) Day 3개 생성 (run 엔티티 연결)
        dayRepository.saveAll(List.of(
                ChallengeDay.create(run, 1, startDate),
                ChallengeDay.create(run, 2, startDate.plusDays(1)),
                ChallengeDay.create(run, 3, startDate.plusDays(2))
        ));

        List<ChallengeDay> days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(run.getId());
        return toResponse(run, days);
    }

    @Transactional(readOnly = true)
    public RunDetailResponse getRunDetail(Long userId, Long runId) {
        ChallengeRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("도전 기록이 없습니다."));

        // run -> goal -> user 소유 검증 (Goal에 user가 연결돼있다는 전제)
        if (!run.getGoal().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        List<ChallengeDay> days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(runId);
        return toResponse(run, days);
    }

    @Transactional
    public RunDetailResponse updateDay(Long userId, Long runId, int dayIndex, UpdateDayRequest req) {
        ChallengeRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("도전 기록이 없습니다."));

        if (!run.getGoal().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        if (run.isEnded()) {
            throw new IllegalStateException("이미 종료된 도전입니다.");
        }

        ChallengeDay day = dayRepository.findByRun_IdAndDayIndex(runId, dayIndex)
                .orElseThrow(() -> new IllegalArgumentException("해당 일차가 없습니다."));

        day.update(req.result(), req.memo(), req.finalizeDay());

        List<ChallengeDay> days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(runId);

        // ====== 종료 판단 ======
        long poorCnt = days.stream().filter(d -> d.getDayResult() == DayResult.PARTIAL).count();
        long failCnt = days.stream().filter(d -> d.getDayResult() == DayResult.FAIL).count();
        long successCnt = days.stream().filter(d -> d.getDayResult() == DayResult.DONE).count();

        // 1) FAIL 하나라도 뜨면 즉시 종료 + FAIL 확정
        if (failCnt >= 1) {
            run.endWithTier(TierStatus.FAIL);
            return toResponse(run, days);
        }

        // 2) POOR 2개 이상이면 즉시 종료 + FAIL 확정
        if (poorCnt >= 2) {
            run.endWithTier(TierStatus.FAIL);
            return toResponse(run, days);
        }

        // 3) 3일 모두 finalize 되면 그때 배지(티어) 확정 + 종료
        boolean allFinalized = days.stream().allMatch(ChallengeDay::isFinalized);
        if (allFinalized) {
            TierStatus finalTier = decideFinalTier(successCnt, poorCnt);
            run.endWithTier(finalTier);
        }

        // 진행중이면 tierStatus는 null 유지(=배지 진행중)
        return toResponse(run, days);
    }

    @Transactional
    public RunDetailResponse giveUp(Long userId, Long runId) {
        ChallengeRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("도전 기록이 없습니다."));

        if (!run.getGoal().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 이미 종료면 그대로 반환
        if (!run.isEnded()) {
            run.endWithTier(TierStatus.FAIL);
        }

        List<ChallengeDay> days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(runId);
        return toResponse(run, days);
    }

    private TierStatus decideFinalTier(long successCnt, long poorCnt) {
        // 성공 3 -> GOLD
        // 성공 2 + 부실 1 -> BRONZE
        // 그 외 -> FAIL
        if (successCnt == 3) return TierStatus.GOLD;
        if (successCnt == 2 && poorCnt == 1) return TierStatus.BRONZE;
        return TierStatus.FAIL;
    }

    private RunDetailResponse toResponse(ChallengeRun run, List<ChallengeDay> days) {
        List<RunDetailResponse.DayDto> dayDtos = days.stream()
                .map(d -> new RunDetailResponse.DayDto(
                        d.getDayIndex(),
                        d.getDayDate(),
                        d.getDayResult(),
                        d.isFinalized(),
                        d.getDayMemo()
                ))
                .toList();

        return new RunDetailResponse(
                run.getId(),
                run.getGoal().getId(),
                run.getGoal().getTitle(),
                run.getStartDate(),
                run.getExpectedEndDate(),
                run.getRunStatus(),
                run.getTierStatus(), // IN_PROGRESS면 null, ENDED면 GOLD/BRONZE/FAIL
                dayDtos
        );
    }
}