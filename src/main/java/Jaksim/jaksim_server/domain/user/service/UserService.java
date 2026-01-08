package Jaksim.jaksim_server.domain.user.service;

import Jaksim.jaksim_server.domain.user.model.User;
import Jaksim.jaksim_server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateByDeviceId(String deviceId) {
        return userRepository.findByDeviceId(deviceId)
                .orElseGet(() -> userRepository.save(User.create(deviceId)));
    }
}
