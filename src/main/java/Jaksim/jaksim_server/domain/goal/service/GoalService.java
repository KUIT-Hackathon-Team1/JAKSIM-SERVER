package Jaksim.jaksim_server.domain.goal.service;

import Jaksim.jaksim_server.domain.goal.client.GeminiClient;
import Jaksim.jaksim_server.domain.goal.model.Goal;
import Jaksim.jaksim_server.domain.goal.model.GoalCategory;
import Jaksim.jaksim_server.domain.goal.repository.GoalRepository;
import Jaksim.jaksim_server.domain.user.model.User;
import Jaksim.jaksim_server.domain.user.repository.UserRepository;
import Jaksim.jaksim_server.domain.user.service.UserService;
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
    public void save(String deviceId, String goalTitle, GoalCategory goalCategory, String intent) {
        User user = userService.getOrCreateByDeviceId(deviceId);
        Goal goal = Goal.create(user, goalTitle, goalCategory, intent);
        goalRepository.save(goal);
    }

    public String getGoalFromAI() {
        return geminiClient.generate();
    }
}
