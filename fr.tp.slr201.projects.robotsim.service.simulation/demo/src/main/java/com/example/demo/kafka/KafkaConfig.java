package com.example.demo.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.tp.inf112.projects.robotsim.model.Factory;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for the simulation microservice.
 * Configures Kafka producers to publish factory simulation events
 * with custom JSON serialization using our configured ObjectMapper.
 */
@Configuration
public class KafkaConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
    
    /**
     * Custom ObjectMapper configured with polymorphic type handling
     * for Factory and Component serialization
     */
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Kafka bootstrap servers address.
     * Default is localhost:9092 for local development with Docker.
     */
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    
    /**
     * Configures the Kafka producer with custom Jackson serializer.
     * 
     * @return Producer factory for Factory objects
     */
    @Bean
    public ProducerFactory<String, Factory> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Kafka broker connection
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        
        // Key serializer (simple string)
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Value serializer (custom JSON with our ObjectMapper)
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Additional producer settings for reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry on transient errors
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // Ensure ordering
        
        logger.info("Configured Kafka producer with bootstrap servers: {}", BOOTSTRAP_SERVERS);
        
        // Create producer factory with custom ObjectMapper
        DefaultKafkaProducerFactory<String, Factory> factory = 
            new DefaultKafkaProducerFactory<>(configProps);
        
        // Set the custom ObjectMapper for JSON serialization
        factory.setValueSerializer(new JsonSerializer<>(objectMapper));
        
        return factory;
    }
    
    /**
     * Creates the KafkaTemplate bean for sending Factory objects to Kafka.
     * 
     * @return KafkaTemplate configured with our producer factory
     */
    @Bean
    public KafkaTemplate<String, Factory> kafkaTemplate() {
        KafkaTemplate<String, Factory> template = new KafkaTemplate<>(producerFactory());
        logger.info("Created KafkaTemplate for Factory simulation events");
        return template;
    }
    
    /**
     * Example topic bean - topics will be created dynamically per factory,
     * but you can define default topics here if needed.
     * 
     * @return A default simulation topic
     */
    @Bean
    public NewTopic defaultSimulationTopic() {
        return TopicBuilder.name("simulation-default")
            .partitions(1)
            .replicas(1)
            .build();
    }
}
