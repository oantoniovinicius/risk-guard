package com.galeritos.risk_guard.identity.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                  "password":"StrongPass123",
                  "role":"USER"
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
}
