package Jaksim.jaksim_server.domain.goal.repository;

import Jaksim.jaksim_server.domain.goal.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    Optional<Goal> findByIdAndUserId(Long goalId, Long userId);

    @Modifying
    @Query("update Goal g set g.isActive = false where g.user.id = :userId and g.isActive = true")
    int deactivateAllActive(@Param("userId") Long userId);
}
