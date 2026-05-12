package com.galeritos.risk_guard.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galeritos.risk_guard.TestcontainersConfiguration;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CreateTransferRequest;
import com.galeritos.risk_guard.config.MessagingProperties;
import com.galeritos.risk_guard.identity.application.usecase.dto.LoginCommand;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.controller.dto.RegisterRequest;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @Autowired
    private MessagingProperties messagingProperties;

    @BeforeEach
    void setUp() throws Exception {
        listenerRegistry.stop();
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(messagingProperties.consumer().transactionCreatedQueue());
            channel.queuePurge(messagingProperties.consumer().transactionAnalyzedQueue());
            channel.queuePurge(messagingProperties.consumer().transactionStatusQueue());
            channel.queuePurge(messagingProperties.consumer().userApprovedQueue());
            return null;
        });

        jdbcTemplate.execute("""
                TRUNCATE TABLE transaction_decision_history, transactions, accounts,
                               admin_decision_history, admin_settings, risk_analysis,
                               user_credentials, users RESTART IDENTITY CASCADE
                """);

        RegisterRequest request = new RegisterRequest(
                "Antonio",
                "antonio@email.com",
                "85826257580",
                "user123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        RegisterRequest receiverRequest = new RegisterRequest(
                "Maria",
                "maria@email.com",
                "52998224725",
                "user123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(receiverRequest)));

        User admin = new User(
                null,
                "Admin",
                "admin@email.com",
                "85826256508",
                Role.ADMIN,
                UserStatus.ACTIVE);

        User savedAdmin = userRepository.save(admin);

        UserCredential adminCredential = new UserCredential(
                null,
                savedAdmin,
                passwordEncoder.encode("admin123"),
                true,
                null);

        userCredentialRepository.save(adminCredential);
        listenerRegistry.start();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        User savedUser = userRepository.findByEmail("antonio@email.com")
                .orElse(null);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("antonio@email.com");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void shouldLoginUserSucessfully() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginCommand("antonio@email.com", "user123"));

        String response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("accessToken").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    void pendingUserShouldNotCreateTransfer() throws Exception {
        User sender = userRepository.findByEmail("antonio@email.com")
                .orElseThrow();

        User receiver = userRepository.findByEmail("maria@email.com")
                .orElseThrow();

        String loginBody = objectMapper.writeValueAsString(
                new LoginCommand("antonio@email.com", "user123"));

        String loginResponse = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        CreateTransferRequest transferRequest = new CreateTransferRequest(sender.getId(), receiver.getId(),
                new BigDecimal("100.00"));

        mockMvc.perform(post("/transfers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldApproveUser() throws Exception {
        User pendingUser = userRepository.findByEmail("antonio@email.com")
                .orElseThrow();

        String loginBody = objectMapper.writeValueAsString(
                new LoginCommand("admin@email.com", "admin123"));

        String loginResponse = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(post("/admin/users/" + pendingUser.getId() + "/approve")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(pendingUser.getId())
                .orElseThrow();

        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
