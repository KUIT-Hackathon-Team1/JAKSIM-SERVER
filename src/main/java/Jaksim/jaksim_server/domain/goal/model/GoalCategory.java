package Jaksim.jaksim_server.domain.goal.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoalCategory {
    HEALTH("health"),
    LANGUAGE("language"),
    EXERCISE("exercise"),
    SELF_DEV("self_development");

    private final String iconKey;
}
