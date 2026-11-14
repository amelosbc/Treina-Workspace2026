package com.amelosbc.taskmanager.application;

import com.amelosbc.taskmanager.application.dto.CurrentUserContext;
import com.amelosbc.taskmanager.domain.User;
import com.amelosbc.taskmanager.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public CurrentUserContext syncAndGetUser(String clerkUserId, String email, String name) {
        User user = userRepository.findByClerkUserId(clerkUserId)
            .orElseGet(() -> {
                User newUser = new User(clerkUserId, email, name);
                return userRepository.save(newUser);
            });

        return CurrentUserContext.fromDomain(user);
    }
}
