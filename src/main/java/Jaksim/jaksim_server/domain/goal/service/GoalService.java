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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import Jaksim.jaksim_server.global.exception.CustomException;
import Jaksim.jaksim_server.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {
    private final GoalRepository goalRepository;
    private final UserService userService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

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

    public List<String> suggestGoals(Long userId, SuggestGoalRequest req) {

        // 난이도 변경이면 baseGoal 검증 후 prompt 구성에 활용
        if (req.baseGoalId() != null) {
            Goal base = goalRepository.findByIdAndUserId(req.baseGoalId(), userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NONE_GOAL));

            return getGoalFromAIAdjust(base.getId(), req.action());
        }

        // 새 목표 생성(카테고리+의도 기반)
        return getGoalFromAINew(req.goalCategory(),req.intent());
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

    public List<String> getGoalFromAINew(GoalCategory goalCategory, String intention) {
        String category = goalCategory.getIconKey();

        String prompt = String.format("""
            당신은 목표 달성을 돕는 AI 코치입니다.
            사용자가 입력한 [카테고리]와 [의도]를 바탕으로 구체적이고 실천 가능한 목표 3가지를 추천해주세요.

            [입력 정보]
            - 카테고리: %s
            - 의도: %s

            [제약 조건]
            1. 목표는 행동 중심적이고 명확해야 합니다.
            2. 다른 말은 절대 하지 말고, 오직 JSON 문자열 배열 포맷으로만 응답하세요.
            3. 응답 예시: ["매일 아침 30분 조깅하기", "주 3회 샐러드 먹기", "엘리베이터 대신 계단 이용하기"]
            """, category, intention);

        String rawResponse = geminiClient.generate(prompt);
        System.out.println(rawResponse);



        return parseResponse(rawResponse);

    }

    public List<String> getGoalFromAIAdjust(Long baseGoalId, DifficultyAction action) {
        Goal baseGoal = goalRepository.findById(baseGoalId)
                .orElseThrow(() -> new IllegalArgumentException("base goal not found. id=" + baseGoalId));

        String title = baseGoal.getTitle();
        String intention = baseGoal.getIntent();
        String difficulty = (action == DifficultyAction.UP) ? "높아진" : "낮아진";

        String prompt = String.format("""
            당신은 목표 달성을 돕는 AI 코치입니다.
            사용자가 입력한 [제목]와 [의도]를 바탕으로 난이도가 %s 구체적이고 실천 가능한 목표 3가지를 추천해주세요.

            [입력 정보]
            - 제목: %s
            - 의도: %s

            [제약 조건]
            1. 목표는 행동 중심적이고 명확해야 합니다.
            2. 다른 말은 절대 하지 말고, 오직 JSON 문자열 배열 포맷으로만 응답하세요.
            3. 응답 예시: ["매일 아침 30분 조깅하기", "주 3회 샐러드 먹기", "엘리베이터 대신 계단 이용하기"]
            """, difficulty, title, intention);

        String rawResponse = geminiClient.generate(prompt);
        System.out.println(rawResponse);

        return parseResponse(rawResponse);

    }

    private List<String> parseResponse(String rawResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawResponse);

            String textContent = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            String jsonString = textContent
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});

        } catch (Exception e) {
            System.out.println("JSON 파싱 실패. 응답값: " + rawResponse);
            throw new RuntimeException("AI 응답 파싱 중 오류 발생", e);
        }
    }

    private int clampDifficulty(int value) {
        return Math.max(1, Math.min(5, value));
    }
}
