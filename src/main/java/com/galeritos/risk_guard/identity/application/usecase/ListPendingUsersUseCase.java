package com.galeritos.risk_guard.identity.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class ListPendingUsersUseCase {
    private final UserRepository userRepository;

    public ListPendingUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> execute() {
        return userRepository.findAllByStatusOrderByCreatedAtAsc(UserStatus.PENDING);
    }
}
