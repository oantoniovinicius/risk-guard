package com.galeritos.risk_guard.identity.application.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.shared.events.EventTypes;

public record UserApprovedEvent(
        String eventId,
        String eventType,
        UUID userId,
        LocalDateTime occurredAt) {

    public static UserApprovedEvent from(User user) {
        return new UserApprovedEvent(
                UUID.randomUUID().toString(),
                EventTypes.USER_APPROVED,
                user.getId(),
                LocalDateTime.now());
    }
}
