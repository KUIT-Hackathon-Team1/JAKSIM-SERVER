package Jaksim.jaksim_server.domain.goal.dto;

import Jaksim.jaksim_server.domain.goal.model.GoalCategory;

public record CreateGoalRequest(
        String goalTitle,
        GoalCategory goalCategory,
        String intent,
        Long baseGoalId,      // NEW면 null
        DifficultyAction action
) {}

