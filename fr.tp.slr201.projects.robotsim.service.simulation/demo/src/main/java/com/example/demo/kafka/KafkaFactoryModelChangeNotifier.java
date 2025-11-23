package com.example.demo.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryModelChangedNotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka-based implementation of FactoryModelChangedNotifier.
 * This class both publishes factory model changes to Kafka topics AND
 * notifies local observers for GUI repaints.
 * 
 * <p>Each factory gets its own Kafka topic named "simulation-{factoryId}"
 * to ensure topic uniqueness and allow multiple factory simulations
 * to run independently.</p>
 */
public class KafkaFactoryModelChangeNotifier implements FactoryModelChangedNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaFactoryModelChangeNotifier.class);
    
    /**
     * The factory model being observed
     */
    private final Factory factoryModel;
    
    /**
     * Kafka template for sending messages
     */
    private final KafkaTemplate<String, Factory> simulationEventTemplate;
    
    /**
     * The Kafka topic name for this factory
     */
    private final String topicName;
    
    /**
     * List of local observers for GUI repaints
     */
    private final List<Observer> localObservers = new ArrayList<>();
    
    /**
     * Creates a new Kafka notifier for the given factory model.
     * 
     * @param factoryModel The factory model to observe
     * @param simulationEventTemplate The Kafka template for sending messages
     */
    public KafkaFactoryModelChangeNotifier(
            final Factory factoryModel,
            final KafkaTemplate<String, Factory> simulationEventTemplate) {
        
        this.factoryModel = factoryModel;
        this.simulationEventTemplate = simulationEventTemplate;
        this.topicName = "simulation-" + factoryModel.getId();
        
        logger.info("Created Kafka notifier for factory '{}' with topic '{}'", 
                   factoryModel.getId(), topicName);
    }
    
    /**
     * {@inheritDoc}
     * 
     * Publishes the factory model to Kafka AND notifies local observers for GUI repaints.
     */
    @Override
    public void notifyObservers() {
        // FIRST: Notify local observers immediately for GUI repaints
        for (final Observer observer : localObservers) {
            try {
                observer.modelChanged();
            } catch (Exception e) {
                logger.error("Error notifying local observer: {}", e.getMessage());
            }
        }
        
        // SECOND: Publish to Kafka for remote viewers (async)
        try {
            // Create a Spring Message with the factory as payload
            final Message<Factory> factoryMessage = MessageBuilder
                .withPayload(factoryModel)
                .setHeader(KafkaHeaders.TOPIC, topicName)
                .build();
            
            // Send the message asynchronously
            final CompletableFuture<SendResult<String, Factory>> sendResult = 
                simulationEventTemplate.send(factoryMessage);
            
            // Handle the result
            sendResult.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send factory update to Kafka topic '{}': {}", 
                               topicName, ex.getMessage(), ex);
                } else {
                    logger.trace("Successfully sent factory update to topic '{}'", topicName);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error publishing to Kafka for factory '{}': {}", 
                       factoryModel.getId(), e.getMessage());
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * Adds observer to local list for GUI repaints.
     */
    @Override
    public boolean addObserver(final Observer observer) {
        if (observer == null) {
            return false;
        }
        logger.debug("Adding local observer to Kafka notifier for factory '{}'", factoryModel.getId());
        return localObservers.add(observer);
    }
    
    /**
     * {@inheritDoc}
     * 
     * Removes observer from local list.
     */
    @Override
    public boolean removeObserver(final Observer observer) {
        if (observer == null) {
            return false;
        }
        logger.debug("Removing local observer from Kafka notifier for factory '{}'", factoryModel.getId());
        return localObservers.remove(observer);
    }
    
    /**
     * Gets the Kafka topic name for this factory.
     * 
     * @return The topic name
     */
    public String getTopicName() {
        return topicName;
    }
}
