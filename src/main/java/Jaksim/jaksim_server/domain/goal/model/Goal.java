package Jaksim.jaksim_server.domain.goal.model;

import Jaksim.jaksim_server.domain.user.model.User;
import Jaksim.jaksim_server.global.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "goals",
        indexes = {
                @Index(name = "idx_goals_user_active", columnList = "user_id,is_active")
        })
public class Goal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "goal_title", nullable = false, length = 50)
    private String title;

    @Column(name = "goal_description", length = 120)
    private String description;

    @Column(name = "goal_intent", length = 200)
    private String intent;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_category", nullable = false, length = 20)
    private GoalCategory category;

    @Column(name = "difficulty_level", nullable = false)
    private int difficultyLevel = 3;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    private Goal(User user, String title, String description, String intent,
                 GoalCategory category, int difficultyLevel) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.intent = intent;
        this.category = category;
        this.difficultyLevel = difficultyLevel;
        this.isActive = true;
    }

    public static Goal create(User user, GoalCategory goalCategory, String intent) {
        return Goal.builder()
                .user(user)
                .title("Hello")
                .intent(intent)
                .category(goalCategory)
                .difficultyLevel(3)
                .build();
    }

    public static Goal create(User user, String goalTitle, GoalCategory goalCategory, String intent, int difficultyLevel) {
        return Goal.builder()
                .user(user)
                .title(goalTitle)
                .category(goalCategory)
                .intent(intent)
                .difficultyLevel(difficultyLevel)
                .build();
    }

    public void deactivate() {
        this.isActive = false;
    }
}
