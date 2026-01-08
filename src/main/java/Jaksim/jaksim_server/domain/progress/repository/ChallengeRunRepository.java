package Jaksim.jaksim_server.domain.progress.repository;

import Jaksim.jaksim_server.domain.progress.model.ChallengeRun;
import Jaksim.jaksim_server.domain.progress.model.enums.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeRunRepository extends JpaRepository<ChallengeRun, Long> {
    Optional<ChallengeRun> findTopByGoal_IdAndRunStatusOrderByStartDateDesc(Long goalId, RunStatus runStatus);
    List<ChallengeRun> findAllByGoal_User_IdOrderByStartDateDesc(Long userId);
}
