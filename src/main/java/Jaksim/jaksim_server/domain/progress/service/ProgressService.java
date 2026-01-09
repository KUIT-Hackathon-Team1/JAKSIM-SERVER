package Jaksim.jaksim_server.domain.progress.service;

import Jaksim.jaksim_server.domain.goal.model.Goal;
import Jaksim.jaksim_server.domain.goal.repository.GoalRepository;
import Jaksim.jaksim_server.domain.progress.dto.*;
import Jaksim.jaksim_server.domain.progress.model.*;
import Jaksim.jaksim_server.domain.progress.model.enums.*;
import Jaksim.jaksim_server.domain.progress.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final int TARGET_DAYS = 3;

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

        if (dayIndex < 1 || dayIndex > 3) {
            throw new IllegalArgumentException("dayIndex는 1~3만 가능합니다.");
        }

        ChallengeDay day = dayRepository.findByRun_IdAndDayIndex(runId, dayIndex)
                .orElseThrow(() -> new IllegalArgumentException("해당 일차가 없습니다."));

        List<ChallengeDay> days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(runId);

        int currentDayIndex = computeCurrentDayIndex(run, days);

        boolean hasMemo = req.memo() != null;
        boolean hasResult = req.result() != null;
        boolean wantsFinalize = req.finalizeDay();

        // 미래일 선택 금지
        if (dayIndex > currentDayIndex) {
            throw new IllegalStateException("아직 진행할 수 없는 일차입니다.");
        }

        // memo 상시 수정가능
        if (hasMemo) {
            day.setDayMemo(req.memo());
        }

        // 여기부터 result/finalize 정책

        // run이 ENDED면 result/finalize 불가 (memo만)
        if (run.isEnded()) {
            if (hasResult || wantsFinalize) {
                throw new IllegalStateException("종료된 도전은 달성 여부를 수정할 수 없습니다.");
            }
            return toResponse(run, days);
        }

        // 과거 일차면 memo만 허용
        if (dayIndex < currentDayIndex) {
            if (hasResult || wantsFinalize) {
                throw new IllegalStateException("지난 일차는 메모만 수정할 수 있습니다.");
            }
            return toResponse(run, days);
        }

        // 현재 일차(dayIndex == currentDayIndex)

        // 이미 확정된 날이면 memo만 허용
        if (day.isFinalized()) {
            if (hasResult || wantsFinalize) {
                throw new IllegalStateException("이미 종료한 일차는 달성 여부를 수정할 수 없습니다.");
            }
            return toResponse(run, days);
        }

        // 현재 일차 + 미확정일 때만 result 변경 허용
        if (hasResult) {
            day.setDayResult(req.result());
        }

        // 하루 끝내기
        if (wantsFinalize) {
            if (day.getDayResult() == null || day.getDayResult() == DayResult.NOT_SET) {
                throw new IllegalArgumentException("하루 끝내기 전에 달성 여부를 선택해야 합니다.");
            }

            day.finalizeDay();

            // ====== 종료 판단 (finalized 된 날들만 기준) ======
            long poorCnt = days.stream()
                    .filter(ChallengeDay::isFinalized)
                    .filter(d -> d.getDayResult() == DayResult.PARTIAL)
                    .count();

            long failCnt = days.stream()
                    .filter(ChallengeDay::isFinalized)
                    .filter(d -> d.getDayResult() == DayResult.FAIL)
                    .count();

            long successCnt = days.stream()
                    .filter(ChallengeDay::isFinalized)
                    .filter(d -> d.getDayResult() == DayResult.DONE)
                    .count();

            // 1) FAIL 하나라도 뜨면 즉시 종료 + FAIL 확정
            if (failCnt >= 1) {
                run.endWithTier(TierStatus.FAIL);
                return toResponse(run, days);
            }

            // 2) PARTIAL 2개 이상이면 즉시 종료 + FAIL 확정
            if (poorCnt >= 2) {
                run.endWithTier(TierStatus.FAIL);
                return toResponse(run, days);
            }

            // 3) 3일 모두 finalize 되면 그때 티어 확정 + 종료
            boolean allFinalized = days.stream().allMatch(ChallengeDay::isFinalized);
            if (allFinalized) {
                TierStatus finalTier = decideFinalTier(successCnt, poorCnt);
                run.endWithTier(finalTier);
            }
        }

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

    private int computeCurrentDayIndex(ChallengeRun run, List<ChallengeDay> days) {
        LocalDate start = run.getStartDate();
        LocalDate today = LocalDate.now();

        int byDate = (int) (ChronoUnit.DAYS.between(start, today) + 1);
        if (byDate < 1) byDate = 1;
        if (byDate > TARGET_DAYS) byDate = TARGET_DAYS;

        if (!run.isEnded()) {
            return byDate;
        }

        int lastFinalized = days.stream()
                .filter(ChallengeDay::isFinalized)
                .mapToInt(ChallengeDay::getDayIndex)
                .max()
                .orElse(1);

        return Math.min(byDate, lastFinalized);
    }

    private RunDetailResponse toResponse(ChallengeRun run, List<ChallengeDay> days) {
        int currentDayIndex = computeCurrentDayIndex(run, days);

        List<RunDetailResponse.DayDto> daysDto = days.stream()
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
                currentDayIndex,
                run.getRunStatus(),
                run.getTierStatus(), // IN_PROGRESS면 null, ENDED면 GOLD/BRONZE/FAIL
                daysDto
        );
    }
}