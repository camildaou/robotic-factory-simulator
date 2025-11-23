package com.example.demo.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import fr.tp.inf112.projects.robotsim.model.Component;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;

/**
 * Configuration class for Spring Boot application beans.
 */
@Configuration
public class ApplicationConfig {
    
    /**
     * Creates a RestTemplate bean for making HTTP requests to external services.
     * 
     * @return RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * Configures Jackson ObjectMapper with polymorphic type handling for factory model classes.
     * This is required for proper serialization/deserialization of Component subclasses.
     * 
     * @return Configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        final PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(PositionedShape.class.getPackageName())
            .allowIfSubType(Component.class.getPackageName())
            .allowIfSubType(BasicVertex.class.getPackageName())
            .allowIfSubType(ArrayList.class.getName())
            .allowIfSubType(LinkedHashSet.class.getName())
            .build();
        
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
        
        return objectMapper;
    }
}