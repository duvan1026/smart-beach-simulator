package com.beachmonitor.cep.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "beach.events";
    public static final String QUEUE_NAME = "beach.sensors";
    public static final String ROUTING_KEY = "beach.combined";

    @Bean
    public TopicExchange beachExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue sensorQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue sensorQueue, TopicExchange beachExchange) {
        return BindingBuilder.bind(sensorQueue).to(beachExchange).with(ROUTING_KEY);
    }
}
