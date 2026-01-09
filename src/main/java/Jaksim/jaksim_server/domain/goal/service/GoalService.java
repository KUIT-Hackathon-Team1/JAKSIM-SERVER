package Jaksim.jaksim_server.domain.goal.service;

import Jaksim.jaksim_server.domain.goal.client.GeminiClient;
import Jaksim.jaksim_server.domain.goal.dto.CreateGoalRequest;
import Jaksim.jaksim_server.domain.goal.dto.DifficultyAction;
import Jaksim.jaksim_server.domain.goal.dto.SuggestGoalRequest;
import Jaksim.jaksim_server.domain.goal.model.Goal;
import Jaksim.jaksim_server.domain.goal.model.GoalCategory;
import Jaksim.jaksim_server.domain.goal.repository.GoalRepository;
import Jaksim.jaksim_server.domain.user.model.User;
import Jaksim.jaksim_server.domain.user.service.UserService;
import Jaksim.jaksim_server.global.exception.CustomException;
import Jaksim.jaksim_server.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoalService {
    private final GoalRepository goalRepository;
    private final UserService userService;
    private final GeminiClient geminiClient;


    @Transactional
    public Long save(String deviceId, CreateGoalRequest req) {
        User user = userService.getOrCreateByDeviceId(deviceId);
        goalRepository.deactivateAllActive(user.getId());

        // 새 목표 저장
        if (req.baseGoalId() == null) {
            Goal goal = Goal.create(user, req.goalTitle(), req.goalCategory(), req.intent());
            goalRepository.save(goal);
            return goal.getId();
        }

        //adjust하기
        Goal base = goalRepository.findByIdAndUserId(req.baseGoalId(), user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NONE_GOAL));
        int delta = (req.action() == DifficultyAction.UP) ? 1 : -1;
        int newLevel = clampDifficulty(base.getDifficultyLevel() + delta);

        Goal goal = Goal.create(
                user,
                req.goalTitle(),
                base.getCategory(),
                req.intent(),
                newLevel
        );



        goalRepository.save(goal);
        return goal.getId();
    }

    public String[] suggestGoals(Long userId, SuggestGoalRequest req) {

        // 난이도 변경이면 baseGoal 검증 후 prompt 구성에 활용
        if (req.baseGoalId() != null) {
            Goal base = goalRepository.findByIdAndUserId(req.baseGoalId(), userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NONE_GOAL));

            return geminiClient.generate(req.goalCategory(),req.intent());
        }

        // 새 목표 생성(카테고리+의도 기반)
        return geminiClient.generate(req.goalCategory(),req.intent());
    }

    @Transactional
    public Long keep(String deviceId, Long baseGoalId) {
        User user = userService.getOrCreateByDeviceId(deviceId);

        Goal base = goalRepository.findByIdAndUserId(baseGoalId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NONE_GOAL));

        goalRepository.deactivateAllActive(user.getId());

        Goal copied = Goal.create(
                user,
                base.getTitle(),
                base.getCategory(),
                base.getIntent(),
                base.getDifficultyLevel()
        );

        goalRepository.save(copied);
        return copied.getId();
    }

    private int clampDifficulty(int value) {
        return Math.max(1, Math.min(5, value));
    }
}
