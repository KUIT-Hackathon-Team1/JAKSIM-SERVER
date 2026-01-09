package Jaksim.jaksim_server.domain.user.service;

import Jaksim.jaksim_server.domain.user.model.User;
import Jaksim.jaksim_server.domain.user.repository.UserRepository;
import Jaksim.jaksim_server.domain.user.dto.UserResponse;
import Jaksim.jaksim_server.global.exception.CustomException;
import Jaksim.jaksim_server.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class UserStatService {
    private final UserRepository userRepository;

    public UserStatService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getUserStats() {
        User user = userRepository.findAll().stream().findFirst().orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int successRate = 0;
        if(user.getTotalGoals() > 0) {
            double rate = (double) user.getSuccessGoals() * 100 /  (user.getTotalGoals() * 3);
            successRate = (int) Math.round(rate);
        }

        return new UserResponse(user.getTotalGoals(), user.getSuccessGoals(), successRate);
    }
}
