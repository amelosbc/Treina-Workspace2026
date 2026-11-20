package com.amelosbc.taskmanager.application.dto;

import com.amelosbc.taskmanager.domain.User;

public record CurrentUserContext(
    Long userId,
    String clerkUserId,
    String email,
    String name
) {
    public static CurrentUserContext fromDomain(User user) {
        return new CurrentUserContext(
            user.getId(),
            user.getClerkUserId(),
            user.getEmail(),
            user.getName()
        );
    }
}
