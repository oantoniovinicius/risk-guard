package com.galeritos.risk_guard.identity.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galeritos.risk_guard.TestcontainersConfiguration;
import com.galeritos.risk_guard.identity.application.security.JwtService;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.PasswordResetTokenRepository;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        userCredentialRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterAsPendingAndAllowLogin() throws Exception {
        String registerBody = """
                {
                  "name":"New User",
                  "email":"new.user@example.com",
                  "document":"12345678912",
                  "password":"StrongPass123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        User persistedUser = userRepository.findByEmail("new.user@example.com").orElseThrow();
        assertEquals(UserStatus.PENDING, persistedUser.getStatus());

        UserCredential persistedCredential = userCredentialRepository.findByUser(persistedUser).orElseThrow();
        assertNotEquals("StrongPass123", persistedCredential.getPasswordHash());
        assertTrue(passwordEncoder.matches("StrongPass123", persistedCredential.getPasswordHash()));

        String loginBody = """
                {
                  "email":"new.user@example.com",
                  "password":"StrongPass123"
                }
                """;

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldAllowAdminToApprovePendingUser() throws Exception {
        User pendingUser = userRepository
                .save(new User(null, "Pending User", "pending.user@example.com", "12345678999", Role.USER,
                        UserStatus.PENDING));
        userCredentialRepository.save(new UserCredential(
                null,
                pendingUser,
                passwordEncoder.encode("StrongPass123"),
                true));

        User admin = userRepository.save(new User(
                null,
                "Admin User",
                "admin@example.com",
                "99988877766",
                Role.ADMIN,
                UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(
                null,
                admin,
                passwordEncoder.encode("AdminPass123"),
                true));

        mockMvc.perform(post("/admin/users/{userId}/approve", pendingUser.getId())
                .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(pendingUser.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        User updated = userRepository.findById(pendingUser.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, updated.getStatus());

        String loginBody = """
                {
                  "email":"pending.user@example.com",
                  "password":"StrongPass123"
                }
                """;

        String response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        assertTrue(node.get("accessToken").asText().length() > 20);
    }

    @Test
    void shouldAllowAdminToDenyPendingUser() throws Exception {
        User pendingUser = userRepository
                .save(new User(null, "Pending User", "pending.deny@example.com", "12345678888", Role.USER,
                        UserStatus.PENDING));
        userCredentialRepository.save(new UserCredential(
                null,
                pendingUser,
                passwordEncoder.encode("StrongPass123"),
                true));

        User admin = userRepository.save(new User(
                null,
                "Admin User",
                "admin.deny@example.com",
                "99988877711",
                Role.ADMIN,
                UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(
                null,
                admin,
                passwordEncoder.encode("AdminPass123"),
                true));

        mockMvc.perform(post("/admin/users/{userId}/deny", pendingUser.getId())
                .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(pendingUser.getId().toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        User updated = userRepository.findById(pendingUser.getId()).orElseThrow();
        assertEquals(UserStatus.REJECTED, updated.getStatus());
    }

    @Test
    void shouldForbidNonAdminToDenyPendingUser() throws Exception {
        User pendingUser = userRepository
                .save(new User(null, "Pending User", "pending.forbidden@example.com", "12345678777", Role.USER,
                        UserStatus.PENDING));
        userCredentialRepository.save(new UserCredential(
                null,
                pendingUser,
                passwordEncoder.encode("StrongPass123"),
                true));

        User analyst = userRepository.save(new User(
                null,
                "Analyst User",
                "analyst.deny@example.com",
                "99988877722",
                Role.ANALYST,
                UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(
                null,
                analyst,
                passwordEncoder.encode("AnalystPass123"),
                true));

        mockMvc.perform(post("/admin/users/{userId}/deny", pendingUser.getId())
                .header("Authorization", "Bearer " + jwtService.generateToken(analyst)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListPendingUsersForAdmin() throws Exception {
        userRepository.save(new User(null, "Pending One", "pending.one@example.com", "12345000111", Role.USER, UserStatus.PENDING));
        userRepository.save(new User(null, "Pending Two", "pending.two@example.com", "12345000112", Role.USER, UserStatus.PENDING));
        userRepository.save(new User(null, "Active One", "active.one@example.com", "12345000113", Role.USER, UserStatus.ACTIVE));

        User admin = userRepository.save(new User(
                null,
                "Admin User",
                "admin.pending.list@example.com",
                "99988870001",
                Role.ADMIN,
                UserStatus.ACTIVE));

        mockMvc.perform(get("/admin/users/pending")
                .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    @Test
    void shouldReturnUserDetailForAdmin() throws Exception {
        User target = userRepository.save(new User(
                null,
                "Detail User",
                "detail.user@example.com",
                "12345000999",
                Role.USER,
                UserStatus.PENDING));

        User admin = userRepository.save(new User(
                null,
                "Admin User",
                "admin.detail@example.com",
                "99988870002",
                Role.ADMIN,
                UserStatus.ACTIVE));

        mockMvc.perform(get("/admin/users/{userId}", target.getId())
                .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(target.getId().toString()))
                .andExpect(jsonPath("$.email").value("detail.user@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldForbidNonAdminToListPendingUsers() throws Exception {
        User analyst = userRepository.save(new User(
                null,
                "Analyst User",
                "analyst.pending.list@example.com",
                "99988870003",
                Role.ANALYST,
                UserStatus.ACTIVE));

        mockMvc.perform(get("/admin/users/pending")
                .header("Authorization", "Bearer " + jwtService.generateToken(analyst)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToSuspendActiveUser() throws Exception {
        User activeUser = userRepository
                .save(new User(null, "Active User", "active.suspend@example.com", "12345000777", Role.USER,
                        UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(
                null,
                activeUser,
                passwordEncoder.encode("StrongPass123"),
                true));

        User admin = userRepository.save(new User(
                null,
                "Admin User",
                "admin.suspend@example.com",
                "99988870004",
                Role.ADMIN,
                UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(
                null,
                admin,
                passwordEncoder.encode("AdminPass123"),
                true));

        mockMvc.perform(post("/admin/users/{userId}/suspend", activeUser.getId())
                .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(activeUser.getId().toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        User updated = userRepository.findById(activeUser.getId()).orElseThrow();
        assertEquals(UserStatus.REJECTED, updated.getStatus());
    }

    @Test
    void shouldForbidNonAdminToSuspendUser() throws Exception {
        User activeUser = userRepository
                .save(new User(null, "Active User", "active.forbidden.suspend@example.com", "12345000778", Role.USER,
                        UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(
                null,
                activeUser,
                passwordEncoder.encode("StrongPass123"),
                true));

        User analyst = userRepository.save(new User(
                null,
                "Analyst User",
                "analyst.suspend@example.com",
                "99988870005",
                Role.ANALYST,
                UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(
                null,
                analyst,
                passwordEncoder.encode("AnalystPass123"),
                true));

        mockMvc.perform(post("/admin/users/{userId}/suspend", activeUser.getId())
                .header("Authorization", "Bearer " + jwtService.generateToken(analyst)))
                .andExpect(status().isForbidden());
    }
}
