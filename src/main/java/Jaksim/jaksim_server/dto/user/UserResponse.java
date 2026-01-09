package Jaksim.jaksim_server.dto.user;

import Jaksim.jaksim_server.domain.user.model.User;

public record UserResponse(
        int complete_loop,
        int successive_success,
        int success_rate
) {
}
