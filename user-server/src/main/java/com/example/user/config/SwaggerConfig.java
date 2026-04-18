package com.example.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI waterOrderingOpenAPI() {
        return new OpenAPI().info(new Info().title("送水便利系统API").version("1.0")
                .description("基于SpringBoot的后端实现")
                .contact(new Contact().name("LL&LY").url("https://toscode.gitee.com/goldget/water-ordering"))
        );
    }

    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder().group("order")
                .pathsToMatch("/order/**")
                .build();
    }

    @Bean
    public GroupedOpenApi buildingApi() {
        return GroupedOpenApi.builder().group("building")
                .pathsToMatch("/building/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder().group("user")
                .pathsToMatch("/user/**")
                .build();
    }

    @Bean
    public GroupedOpenApi workerApi() {
        return GroupedOpenApi.builder().group("worker")
                .pathsToMatch("/worker/**")
                .build();
    }

    @Bean
    public GroupedOpenApi loginApi() {
        return GroupedOpenApi.builder().group("login")
                .pathsToMatch("/login/**")
                .build();
    }
}
