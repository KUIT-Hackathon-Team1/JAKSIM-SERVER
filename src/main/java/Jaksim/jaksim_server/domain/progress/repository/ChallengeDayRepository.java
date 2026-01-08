package Jaksim.jaksim_server.domain.progress.repository;

import Jaksim.jaksim_server.domain.progress.model.ChallengeDay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChallengeDayRepository extends JpaRepository<ChallengeDay, Long> {
    List<ChallengeDay> findAllByRun_IdOrderByDayIndexAsc(Long runId);
    Optional<ChallengeDay> findByRun_IdAndDayIndex(Long runId, int dayIndex);
}
