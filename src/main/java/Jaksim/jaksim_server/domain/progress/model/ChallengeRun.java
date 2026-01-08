package Jaksim.jaksim_server.domain.progress.model;

import Jaksim.jaksim_server.domain.goal.model.Goal;
import Jaksim.jaksim_server.domain.progress.model.enums.RunStatus;
import Jaksim.jaksim_server.domain.progress.model.enums.TierStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "challenge_runs",
        indexes = {
                @Index(name = "idx_runs_goal_status", columnList = "goal_id,run_status")
        }
)
public class ChallengeRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long id;

    // ✅ FK 생성 대상: goal_id -> goals.goal_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "goal_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_runs_goal_id")
    )
    private Goal goal;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expected_end_date", nullable = false)
    private LocalDate expectedEndDate;

    @Column(name = "target_days", nullable = false)
    private int targetDays = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_status", nullable = false, length = 20)
    private RunStatus runStatus = RunStatus.IN_PROGRESS;

    // ✅ 진행중이면 null(배지=진행중), 종료될 때만 확정
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_status", nullable = true, length = 20)
    private TierStatus tierStatus;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder
    private ChallengeRun(Goal goal, LocalDate startDate) {
        this.goal = goal;
        this.startDate = startDate;
        this.expectedEndDate = startDate.plusDays(2);
        this.targetDays = 3;
        this.runStatus = RunStatus.IN_PROGRESS;
        this.tierStatus = null;
        this.endedAt = null;
    }

    public static ChallengeRun start(Goal goal, LocalDate startDate) {
        LocalDate s = (startDate != null) ? startDate : LocalDate.now();
        return ChallengeRun.builder()
                .goal(goal)
                .startDate(s)
                .build();
    }

    public boolean isInProgress() {
        return runStatus == RunStatus.IN_PROGRESS;
    }

    public boolean isEnded() {
        return runStatus == RunStatus.ENDED;
    }

    // ✅ 종료 시점에만 티어 확정
    public void endWithTier(TierStatus tier) {
        if (this.runStatus == RunStatus.ENDED) return;
        this.runStatus = RunStatus.ENDED;
        this.tierStatus = tier;
        this.endedAt = LocalDateTime.now();
    }
}