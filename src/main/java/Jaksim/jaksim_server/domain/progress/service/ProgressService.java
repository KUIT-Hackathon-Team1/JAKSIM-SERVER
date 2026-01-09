package Jaksim.jaksim_server.domain.progress.service;

import Jaksim.jaksim_server.domain.goal.model.Goal;
import Jaksim.jaksim_server.domain.goal.repository.GoalRepository;
import Jaksim.jaksim_server.domain.progress.dto.*;
import Jaksim.jaksim_server.domain.progress.model.*;
import Jaksim.jaksim_server.domain.progress.model.enums.*;
import Jaksim.jaksim_server.domain.progress.repository.*;
import Jaksim.jaksim_server.global.exception.CustomException;
import Jaksim.jaksim_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
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
    public RunDetailResponse startRun(Long userId) {
        // 1) Goal 소유 검증 포함해서 조회 (메서드명은 GoalRepository에 맞춰 조정)
        Goal goal = goalRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NONE_GOAL));

        // 2) 목표당 진행중 run 1개 제한
        runRepository.findTopByGoal_IdAndRunStatusOrderByStartDateDesc(goal.getId(), RunStatus.IN_PROGRESS)
                .ifPresent(r -> { throw new CustomException(ErrorCode.ALREADY_PROGRESS); });

        LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

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
                .orElseThrow(() -> new CustomException(ErrorCode.NONE_RECORD));

        // run -> goal -> user 소유 검증 (Goal에 user가 연결돼있다는 전제)
        if (!run.getGoal().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        List<ChallengeDay> days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(runId);
        return toResponse(run, days);
    }

    @Transactional
    public RunDetailResponse updateDay(Long userId, Long runId, int dayIndex, UpdateDayRequest req) {
        ChallengeRun run = runRepository.findById(runId)
                .orElseThrow(() -> new CustomException(ErrorCode.NONE_RECORD));

        if (!run.getGoal().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (dayIndex < 1 || dayIndex > 3) {
            throw new CustomException(ErrorCode.INVALID_DAYINDEX_RANGE);
        }

        ChallengeDay day = dayRepository.findByRun_IdAndDayIndex(runId, dayIndex)
                .orElseThrow(() -> new CustomException(ErrorCode.NONE_DATE));

        List<ChallengeDay> days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(runId);

        int currentDayIndex = computeCurrentDayIndex(run, days);

        boolean hasMemo = req.memo() != null;
        boolean hasResult = req.result() != null;
        boolean wantsFinalize = req.finalizeDay();

        if (hasResult) {
            day.setDayResult(req.result());
            dayRepository.saveAndFlush(day); // 시연/안정성 위해 강추
        }
        day.apply(req);

        // 하루 끝내기
        if (wantsFinalize) {
            if (day.getDayResult() == null || day.getDayResult() == DayResult.NOT_SET) {
                throw new CustomException(ErrorCode.MUST_SELECT_BEFORE_DAY);
            }

            day.finalizeDay();

            dayRepository.save(day);
            dayRepository.flush();

            days = dayRepository.findAllByRun_IdOrderByDayIndexAsc(runId);

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

                runRepository.save(run);
                runRepository.flush();

                return toResponse(run, days);
            }

            // 2) PARTIAL 2개 이상이면 즉시 종료 + FAIL 확정
            if (poorCnt >= 2) {
                run.endWithTier(TierStatus.FAIL);

                runRepository.save(run);
                runRepository.flush();

                return toResponse(run, days);
            }

            // 3) 3일 모두 finalize 되면 그때 티어 확정 + 종료
            boolean allFinalized = days.stream().allMatch(ChallengeDay::isFinalized);
            if (allFinalized) {
                TierStatus finalTier = decideFinalTier(successCnt, poorCnt);
                run.endWithTier(finalTier);

                runRepository.save(run);
                runRepository.flush();
            }
        }

        // 미래일 선택 금지
        if (dayIndex > currentDayIndex) {
            throw new CustomException(ErrorCode.FUTURE_SELECTION);
        }

        // memo 상시 수정가능
        if (hasMemo) {
            day.setDayMemo(req.memo());
        }

        // 여기부터 result/finalize 정책

        // run이 ENDED면 result/finalize 불가 (memo만)
        if (run.isEnded()) {
            if (hasResult || wantsFinalize) {
                throw new CustomException(ErrorCode.ACCESS_TO_FINISHED);
            }
            return toResponse(run, days);
        }

        // 과거 일차면 memo만 허용
        if (dayIndex < currentDayIndex) {
            if (hasResult || wantsFinalize) {
                throw new CustomException(ErrorCode.PAST_ONLY_MEMO);
            }
            return toResponse(run, days);
        }

        // 현재 일차(dayIndex == currentDayIndex)

        // 이미 확정된 날이면 memo만 허용
        if (day.isFinalized()) {
            if (hasResult || wantsFinalize) {
                throw new CustomException(ErrorCode.PAST_ACCESS);
            }
            return toResponse(run, days);
        }

        // 현재 일차 + 미확정일 때만 result 변경 허용
        if (hasResult) {
            day.setDayResult(req.result());
        }



        return toResponse(run, days);
    }

    @Transactional
    public RunDetailResponse giveUp(Long userId, Long runId) {
        ChallengeRun run = runRepository.findById(runId)
                .orElseThrow(() -> new CustomException(ErrorCode.NONE_RECORD));

        if (!run.getGoal().getUser().getId().equals(userId)) {
            throw  new CustomException(ErrorCode.FORBIDDEN_ACCESS);
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
        int targetDays = run.getTargetDays();

        int lastFinalized = days.stream()
                .filter(ChallengeDay::isFinalized)
                .mapToInt(ChallengeDay::getDayIndex)
                .max()
                .orElse(0); // 아무것도 확정 안 됐으면 0

        if (!run.isEnded()) {
            // ✅ "하루 끝내기" 하면 즉시 다음 일차로 넘어가게
            return Math.min(targetDays, lastFinalized + 1);
        }

        // ENDED면 마지막 확정일까지만 보여주기(없으면 1)
        return Math.max(1, Math.min(targetDays, lastFinalized == 0 ? 1 : lastFinalized));
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
                run.getGoal().getIntent(),
                run.getGoal().getCategory(),
                run.getGoal().getCategory().getIconKey(),
                run.getStartDate(),
                run.getExpectedEndDate(),
                currentDayIndex,
                run.getRunStatus(),
                run.getTierStatus(), // IN_PROGRESS면 null, ENDED면 GOLD/BRONZE/FAIL
                daysDto
        );
    }
}