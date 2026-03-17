package com.galeritos.risk_guard.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMqConfig {

    @Bean
    TopicExchange transactionEventsExchange(MessagingProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    Queue transactionCreatedQueue(MessagingProperties properties) {
        return new Queue(properties.consumer().transactionCreatedQueue(), true);
    }

    @Bean
    Binding transactionCreatedBinding(
            Queue transactionCreatedQueue,
            TopicExchange transactionEventsExchange,
            MessagingProperties properties) {
        return BindingBuilder.bind(transactionCreatedQueue)
                .to(transactionEventsExchange)
                .with(properties.routing().transactionCreated());
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
