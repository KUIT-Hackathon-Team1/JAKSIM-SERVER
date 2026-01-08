package Jaksim.jaksim_server.domain.progress.model;

import Jaksim.jaksim_server.domain.progress.model.enums.DayResult;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "challenge_days",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_days_run_day_index",
                columnNames = {"run_id", "day_index"}
        ),
        indexes = {
                @Index(name = "idx_days_run", columnList = "run_id")
        }
)
public class ChallengeDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "day_id")
    private Long id;

    // ✅ FK 생성 대상: run_id -> challenge_runs.run_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "run_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_days_run_id")
    )
    private ChallengeRun run;

    @Column(name = "day_index", nullable = false)
    private int dayIndex; // 1~3

    @Column(name = "day_date", nullable = false)
    private LocalDate dayDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_result", nullable = false, length = 20)
    private DayResult dayResult = DayResult.NOT_SET;

    @Column(name = "day_memo", length = 100)
    private String dayMemo;

    @Column(name = "is_finalized", nullable = false)
    private boolean isFinalized = false;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Builder
    private ChallengeDay(ChallengeRun run, int dayIndex, LocalDate dayDate) {
        this.run = run;
        this.dayIndex = dayIndex;
        this.dayDate = dayDate;
        this.dayResult = DayResult.NOT_SET;
        this.isFinalized = false;
        this.finalizedAt = null;
    }

    public static ChallengeDay create(ChallengeRun run, int dayIndex, LocalDate dayDate) {
        return ChallengeDay.builder()
                .run(run)
                .dayIndex(dayIndex)
                .dayDate(dayDate)
                .build();
    }

    public void update(DayResult result, String memo, boolean finalizeDay) {
        this.dayResult = result;
        this.dayMemo = memo;

        if (finalizeDay) {
            this.isFinalized = true;
            this.finalizedAt = LocalDateTime.now();
        }
    }
}
