package com.galeritos.risk_guard.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
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
    Queue transactionAnalyzedQueue(MessagingProperties properties) {
        return new Queue(properties.consumer().transactionAnalyzedQueue(), true);
    }

    @Bean
    Queue transactionStatusQueue(MessagingProperties properties) {
        return new Queue(properties.consumer().transactionStatusQueue(), true);
    }

    @Bean
    Binding transactionCreatedBinding(
            @Qualifier("transactionCreatedQueue") Queue queue,
            TopicExchange transactionEventsExchange,
            MessagingProperties properties) {
        return BindingBuilder.bind(queue)
                .to(transactionEventsExchange)
                .with(properties.routing().transactionCreated());
    }

    @Bean
    Binding transactionAnalyzedBinding(
            @Qualifier("transactionAnalyzedQueue") Queue queue,
            TopicExchange transactionEventsExchange,
            MessagingProperties properties) {
        return BindingBuilder.bind(queue)
                .to(transactionEventsExchange)
                .with(properties.routing().transactionAnalyzed());
    }

    @Bean
    Binding transactionStatusBinding(
            @Qualifier("transactionStatusQueue") Queue queue,
            TopicExchange transactionEventsExchange,
            MessagingProperties properties) {
        return BindingBuilder.bind(queue)
                .to(transactionEventsExchange)
                .with(properties.routing().transactionStatus());
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
