package com.galeritos.risk_guard.banking.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galeritos.risk_guard.TestcontainersConfiguration;
import com.galeritos.risk_guard.banking.infrastructure.messaging.RabbitMessagingProperties;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TransferControllerIntegrationTest.RabbitTestConfig.class })
class TransferControllerIntegrationTest {
    private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(5);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private Queue transactionCreatedTestQueue;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        rabbitAdmin.purgeQueue(transactionCreatedTestQueue.getName(), true);
    }

    @Test
    void shouldCreateTransferReserveBalanceAndPublishTransactionCreatedMessage() throws Exception {
        User sender = userRepository.save(new User(null, "Alice Sender", "alice@example.com", "12345678901", Role.USER, UserStatus.ACTIVE));
        User receiver = userRepository.save(new User(null, "Bob Receiver", "bob@example.com", "10987654321", Role.USER, UserStatus.ACTIVE));
        UUID senderId = sender.getId();
        UUID receiverId = receiver.getId();

        accountRepository.save(new com.galeritos.risk_guard.banking.domain.model.Account(
                null,
                senderId,
                new BigDecimal("500.00"),
                BigDecimal.ZERO));

        String requestBody = objectMapper.writeValueAsString(new TransferRequestPayload(senderId, receiverId, new BigDecimal("125.00")));

        mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderId").value(senderId.toString()))
                .andExpect(jsonPath("$.receiverId").value(receiverId.toString()))
                .andExpect(jsonPath("$.amount").value(125.00))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        com.galeritos.risk_guard.banking.domain.model.Account senderAccount = accountRepository.findByUserId(senderId)
                .orElseThrow();
        assertEquals(0, senderAccount.getBalance().compareTo(new BigDecimal("375.00")));
        assertEquals(0, senderAccount.getReservedBalance().compareTo(new BigDecimal("125.00")));

        Message message = awaitMessage();
        JsonNode eventPayload = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));

        assertNotNull(eventPayload.get("transactionId").asText());
        assertEquals(senderId.toString(), eventPayload.get("senderId").asText());
        assertEquals(receiverId.toString(), eventPayload.get("receiverId").asText());
        assertEquals(0, new BigDecimal("125.00").compareTo(eventPayload.get("amount").decimalValue()));
        assertEquals("CREATED", eventPayload.get("status").asText());
        assertEquals("RESERVED", eventPayload.get("financialStatus").asText());
        assertNotNull(eventPayload.get("createdAt").asText());
    }

    private Message awaitMessage() throws InterruptedException {
        long deadline = System.nanoTime() + MESSAGE_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            Message message = rabbitTemplate.receive(transactionCreatedTestQueue.getName());
            if (message != null) {
                return message;
            }

            Thread.sleep(100);
        }

        throw new AssertionError("Timed out waiting for RabbitMQ message");
    }

    record TransferRequestPayload(UUID senderId, UUID receiverId, BigDecimal amount) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RabbitTestConfig {
        @Bean
        Queue transactionCreatedTestQueue() {
            return new AnonymousQueue();
        }

        @Bean
        Binding transactionCreatedTestBinding(
                Queue transactionCreatedTestQueue,
                TopicExchange transactionEventsExchange,
                RabbitMessagingProperties properties) {
            return BindingBuilder.bind(transactionCreatedTestQueue)
                    .to(transactionEventsExchange)
                    .with(properties.routingKey());
        }
    }
}
