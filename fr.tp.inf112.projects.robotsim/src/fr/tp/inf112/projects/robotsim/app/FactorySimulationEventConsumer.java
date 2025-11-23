package fr.tp.inf112.projects.robotsim.app;

import fr.tp.inf112.projects.robotsim.model.Factory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Kafka consumer that receives factory simulation events and updates
 * the RemoteSimulatorController with fresh factory model data.
 * 
 * <p>This class consumes JSON-serialized factory objects from Kafka topics
 * and passes them to the controller for parsing and view updates.</p>
 */
public class FactorySimulationEventConsumer {
    
    private static final Logger LOGGER = Logger.getLogger(FactorySimulationEventConsumer.class.getName());
    
    /**
     * Kafka consumer for reading simulation events
     */
    private final KafkaConsumer<String, String> consumer;
    
    /**
     * Remote simulator controller to update with received events
     */
    private final RemoteSimulatorController controller;
    
    /**
     * Creates a new Kafka event consumer for the given controller.
     * 
     * @param controller The remote simulator controller to update with events
     */
    public FactorySimulationEventConsumer(final RemoteSimulatorController controller) {
        this.controller = controller;
        
        // Get default consumer properties
        final Properties props = SimulationServiceUtils.getDefaultConsumerProperties();
        
        // Configure deserializers for key and value
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Create Kafka consumer
        this.consumer = new KafkaConsumer<>(props);
        
        // Subscribe to the topic for this factory
        final Factory factory = (Factory) controller.getCanvas();
        final String topicName = SimulationServiceUtils.getTopicName(factory);
        this.consumer.subscribe(Collections.singletonList(topicName));
        
        LOGGER.info("Created Kafka consumer subscribed to topic: " + topicName);
    }
    
    /**
     * Continuously polls Kafka for simulation events and updates the controller.
     * This method blocks until the simulation is stopped.
     * 
     * <p>Each received event contains a JSON-serialized Factory object which is
     * passed to the controller for parsing and view notification.</p>
     */
    public void consumeMessages() {
        try {
            LOGGER.info("Starting Kafka message consumption loop");
            
            while (controller.isAnimationRunning()) {
                // Poll Kafka for new records (wait up to 100ms)
                final ConsumerRecords<String, String> records = 
                    consumer.poll(Duration.ofMillis(100));
                
                // Process each received record
                for (final ConsumerRecord<String, String> record : records) {
                    LOGGER.fine("Received JSON Factory text from topic '" + record.topic() + 
                              "' at offset " + record.offset());
                    
                    // Pass the JSON text to the controller for parsing and view update
                    controller.setCanvasFromJson(record.value());
                }
            }
            
            LOGGER.info("Kafka message consumption loop stopped");
            
        } finally {
            // Always close the consumer to release resources
            consumer.close();
            LOGGER.info("Kafka consumer closed");
        }
    }
}
