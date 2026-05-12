package com.galeritos.risk_guard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galeritos.risk_guard.TestcontainersConfiguration;
import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CreateTransferRequest;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.config.MessagingProperties;
import com.galeritos.risk_guard.identity.application.usecase.dto.LoginCommand;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private AccountRepository accountRepository;

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

    private User sender;
    private User receiver;
    private String senderToken;

    @BeforeEach
    void setUp() throws Exception {
        // Stop listeners so no in-flight message can race with the truncation below.
        // Any message already being processed will finish before stop() returns.
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

        sender = userRepository.save(new User(null, "Antonio", "antonio@email.com", "85826257580",
                Role.USER, UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(null, sender,
                passwordEncoder.encode("user123"), true, null));
        accountRepository.save(new Account(null, sender.getId(), new BigDecimal("1000.00"), BigDecimal.ZERO));

        receiver = userRepository.save(new User(null, "Maria", "maria@email.com", "52998224725",
                Role.USER, UserStatus.ACTIVE));
        userCredentialRepository.save(new UserCredential(null, receiver,
                passwordEncoder.encode("user123"), true, null));
        accountRepository.save(new Account(null, receiver.getId(), new BigDecimal("500.00"), BigDecimal.ZERO));

        String loginBody = objectMapper.writeValueAsString(new LoginCommand("antonio@email.com", "user123"));
        String loginResponse = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        senderToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        listenerRegistry.start();
    }

    @Test
    void activeUserShouldCreateTransferSuccessfully() throws Exception {
        CreateTransferRequest request = new CreateTransferRequest(sender.getId(), receiver.getId(),
                new BigDecimal("200.00"));

        String response = mockMvc.perform(post("/transfers")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var body = objectMapper.readTree(response);
        assertThat(body.get("transactionId").asText()).isNotBlank();
        assertThat(body.get("status").asText()).isEqualTo("CREATED");
        assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(UUID.fromString(body.get("senderId").asText())).isEqualTo(sender.getId());
        assertThat(UUID.fromString(body.get("receiverId").asText())).isEqualTo(receiver.getId());
    }

    @Test
    void insufficientBalanceShouldReturnBadRequest() throws Exception {
        CreateTransferRequest request = new CreateTransferRequest(sender.getId(), receiver.getId(),
                new BigDecimal("9999.00"));

        mockMvc.perform(post("/transfers")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void selfTransferShouldReturnBadRequest() throws Exception {
        CreateTransferRequest request = new CreateTransferRequest(sender.getId(), sender.getId(),
                new BigDecimal("100.00"));

        mockMvc.perform(post("/transfers")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferToBlockedUserShouldReturnBadRequest() throws Exception {
        receiver.block();
        userRepository.save(receiver);

        CreateTransferRequest request = new CreateTransferRequest(sender.getId(), receiver.getId(),
                new BigDecimal("100.00"));

        mockMvc.perform(post("/transfers")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void senderImpersonationShouldReturnForbidden() throws Exception {
        CreateTransferRequest request = new CreateTransferRequest(receiver.getId(), sender.getId(),
                new BigDecimal("100.00"));

        mockMvc.perform(post("/transfers")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
