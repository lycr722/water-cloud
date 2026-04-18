package com.example.order.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbtiMQconfig {
    @Bean
    public MessageConverter jsonMessageConvert(){
        return new Jackson2JsonMessageConverter();
    }
}
