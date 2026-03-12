package com.galeritos.risk_guard.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.galeritos.risk_guard.banking.infrastructure.messaging.RabbitMessagingProperties;

@Configuration
@EnableConfigurationProperties(RabbitMessagingProperties.class)
public class RabbitMqConfig {

    @Bean
    TopicExchange transactionEventsExchange(RabbitMessagingProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
