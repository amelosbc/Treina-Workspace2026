package com.amelosbc.taskmanager.application;

import com.amelosbc.taskmanager.application.dto.CurrentUserContext;
import com.amelosbc.taskmanager.domain.User;
import com.amelosbc.taskmanager.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OnboardingService onboardingService;

    public UserService(UserRepository userRepository, OnboardingService onboardingService) {
        this.userRepository = userRepository;
        this.onboardingService = onboardingService;
    }

    @Transactional
    public CurrentUserContext syncAndGetUser(String clerkUserId, String email, String name) {
        User user = userRepository.findByClerkUserId(clerkUserId)
            .orElseGet(() -> {
                User newUser = new User(clerkUserId, email, name);
                User savedUser = userRepository.save(newUser);
                onboardingService.createInitialBoardForUser(savedUser);
                return savedUser;
            });

        return CurrentUserContext.fromDomain(user);
    }
}

