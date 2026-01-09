package Jaksim.jaksim_server.domain.goal.dto;

import Jaksim.jaksim_server.domain.goal.model.Goal;
import Jaksim.jaksim_server.domain.goal.model.GoalCategory;

public record TitleAndIntentResponse(
        String title,
        String intent
) {
    public static TitleAndIntentResponse from(Goal goal) {
        return new TitleAndIntentResponse(
                goal.getTitle(),
                goal.getIntent()
        );
    }
}
