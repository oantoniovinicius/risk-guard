package com.galeritos.risk_guard.admin.application.usecase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class ListUsersUseCase {

    private final UserRepository repository;

    public ListUsersUseCase(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<User> execute(UserStatus status, Role role, int page, int size) {
        List<Specification<User>> specs = new ArrayList<>();
        if (status != null) specs.add((root, q, cb) -> cb.equal(root.get("status"), status));
        if (role != null) specs.add((root, q, cb) -> cb.equal(root.get("role"), role));

        Specification<User> combined = Specification.allOf(specs);
        return repository.findAll(combined, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }
}
