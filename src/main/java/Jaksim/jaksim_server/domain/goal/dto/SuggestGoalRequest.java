package Jaksim.jaksim_server.domain.goal.dto;

import Jaksim.jaksim_server.domain.goal.model.GoalCategory;

public record SuggestGoalRequest(
        GoalCategory goalCategory,   // NEW면 필수, ADJUST/KEEP면 baseGoalId로부터 가져와도 됨
        String intent,               // 사용자가 입력한 의도(수정 가능)
        Long baseGoalId,             // 난이도 조절/유지하기일 때
        DifficultyAction action      // NEW면 null, 조절이면 UP/DOWN
) {}

