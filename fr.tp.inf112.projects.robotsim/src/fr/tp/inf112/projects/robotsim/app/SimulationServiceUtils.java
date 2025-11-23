package fr.tp.inf112.projects.robotsim.app;

import fr.tp.inf112.projects.robotsim.model.Factory;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;

/**
 * Utility class providing common configuration for Kafka simulation services.
 * Contains methods to generate topic names and default consumer properties.
 */
public class SimulationServiceUtils {
    
    /**
     * Kafka broker address
     */
    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    
    /**
     * Consumer group ID for factory simulation consumers
     */
    private static final String GROUP_ID = "Factory-Simulation-Group";
    
    /**
     * Auto offset reset strategy - start from earliest available message
     */
    private static final String AUTO_OFFSET_RESET = "earliest";
    
    /**
     * Topic name prefix for simulation events
     */
    private static final String TOPIC = "simulation-";
    
    /**
     * Generates the Kafka topic name for a factory model.
     * 
     * @param factoryModel The factory model
     * @return The topic name (e.g., "simulation-factory123.factory")
     */
    public static String getTopicName(final Factory factoryModel) {
        return TOPIC + factoryModel.getId();
    }
    
    /**
     * Creates default Kafka consumer properties.
     * 
     * @return Properties configured for Kafka consumer
     */
    public static Properties getDefaultConsumerProperties() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);
        
        return props;
    }
}
