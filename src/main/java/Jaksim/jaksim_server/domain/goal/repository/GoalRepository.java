package Jaksim.jaksim_server.domain.goal.repository;

import Jaksim.jaksim_server.domain.goal.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    Optional<Goal> findByIdAndUserId(Long goalId, Long userId);
}
