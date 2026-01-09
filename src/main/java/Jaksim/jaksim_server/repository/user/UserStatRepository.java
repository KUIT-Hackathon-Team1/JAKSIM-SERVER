package Jaksim.jaksim_server.repository.user;

import Jaksim.jaksim_server.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatRepository extends JpaRepository<User, Long> {
}
