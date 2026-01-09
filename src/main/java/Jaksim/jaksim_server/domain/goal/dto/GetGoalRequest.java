package Jaksim.jaksim_server.domain.goal.dto;

import Jaksim.jaksim_server.domain.goal.model.GoalCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
public class GetGoalRequest {
    private GoalCategory goalCategory;
    private String intent;
}
