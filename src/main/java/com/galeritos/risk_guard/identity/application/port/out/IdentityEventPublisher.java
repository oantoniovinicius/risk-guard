package com.galeritos.risk_guard.identity.application.port.out;

import com.galeritos.risk_guard.identity.application.event.UserApprovedEvent;

public interface IdentityEventPublisher {
    void publishUserApproved(UserApprovedEvent event);
}
