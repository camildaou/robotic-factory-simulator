package com.example.demo.service;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.kafka.KafkaFactoryModelChangeNotifier;

import fr.tp.inf112.projects.canvas.view.CanvasViewer;
import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import fr.tp.inf112.projects.robotsim.app.SimulatorController;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryPersistenceManager;

/**
 * Service class for managing robot factory simulations.
 * This class provides business logic for starting, stopping, and managing factory simulations.
 */
@Service
public class SimulationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);
    
    /**
     * Map to store currently running simulator controllers by factory ID
     */
    private final Map<String, SimulatorController> runningSimulators = new ConcurrentHashMap<>();
    
    /**
     * Persistence manager for loading/saving factory models (initialized after directory is set)
     */
    private FactoryPersistenceManager persistenceManager;
    
    /**
     * Local directory for factory files
     */
    @Value("${simulation.persistence.local.directory:../../fr.tp.inf112.projects.robotsim}")
    private String localFactoryDirectory;
    
    /**
     * REST template for calling the persistence web server
     */
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Kafka template for publishing factory events
     */
    @Autowired
    private KafkaTemplate<String, Factory> kafkaTemplate;
    
    /**
     * URL of the persistence web server (from application.yml)
     */
    @Value("${simulation.persistence.server.url}")
    private String persistenceServerUrl;
    
    public SimulationService() {
        // Persistence manager will be initialized in @PostConstruct after directory is set
    }
    
    /**
     * Sets the working directory and initializes the persistence manager after Spring initialization
     */
    @PostConstruct
    public void initializeFileChooser() {
        // CRITICAL: Check and initialize AWT/Swing for GUI windows
        try {
            if (GraphicsEnvironment.isHeadless()) {
                logger.error("!!! HEADLESS MODE DETECTED - GUI WINDOWS WILL NOT WORK !!!");
                logger.error("!!! Run with -Djava.awt.headless=false to enable GUI !!!");
            } else {
                logger.info("!!! Graphics environment available - GUI windows enabled");
                // Force initialize the AWT event queue
                SwingUtilities.invokeLater(() -> {
                    logger.info("!!! AWT Event Dispatch Thread initialized and running");
                });
            }
        } catch (Exception e) {
            logger.error("Error checking graphics environment: {}", e.getMessage());
        }
        
        try {
            // Get absolute path
            String baseDir = System.getProperty("user.dir");
            java.io.File factoryDir = new java.io.File(baseDir, localFactoryDirectory).getCanonicalFile();
            
            if (!factoryDir.exists()) {
                logger.warn("Factory directory does not exist: {}", factoryDir.getAbsolutePath());
                factoryDir = new java.io.File("D:\\Users\\Camil\\Downloads\\robotic-factory-simulator\\fr.tp.inf112.projects.robotsim");
            }
            
            if (factoryDir.exists() && factoryDir.isDirectory()) {
                logger.info("Factory file directory set to: {}", factoryDir.getAbsolutePath());
                
                // List files in directory for debugging
                String[] files = factoryDir.list((dir, name) -> name.endsWith(".factory"));
                logger.info("Found {} factory files: {}", files != null ? files.length : 0, 
                           files != null ? String.join(", ", files) : "none");
                
                // Change working directory
                System.setProperty("user.dir", factoryDir.getAbsolutePath());
                
                // Create persistence manager
                FileCanvasChooser canvasChooser = new FileCanvasChooser("factory", "Factory Files");
                this.persistenceManager = new FactoryPersistenceManager(canvasChooser);
                
                logger.info("Persistence manager initialized");
            } else {
                logger.error("Factory directory is not valid: {}", factoryDir.getAbsolutePath());
                FileCanvasChooser canvasChooser = new FileCanvasChooser("factory", "Factory Files");
                this.persistenceManager = new FactoryPersistenceManager(canvasChooser);
            }
        } catch (Exception e) {
            logger.error("Error setting factory directory: {}", e.getMessage(), e);
            FileCanvasChooser canvasChooser = new FileCanvasChooser("factory", "Factory Files");
            this.persistenceManager = new FactoryPersistenceManager(canvasChooser);
        }
    }
    
    /**
     * Starts a simulation for the specified factory ID.
     * 
     * @param factoryId The ID of the factory to simulate
     * @return true if the simulation started successfully, false otherwise
     */
    public boolean startSimulation(String factoryId) {
        logger.info("Attempting to start simulation for factory ID: {}", factoryId);
        
        try {
            // Check if simulation is already running
            if (runningSimulators.containsKey(factoryId)) {
                logger.warn("Simulation for factory ID {} is already running", factoryId);
                return false;
            }
            
            // Load the factory model from persistence
            Factory factoryModel = loadFactoryModel(factoryId);
            if (factoryModel == null) {
                logger.error("Failed to load factory model with ID: {}", factoryId);
                return false;
            }
            
            // Set the Kafka notifier for this factory so events are published
            KafkaFactoryModelChangeNotifier kafkaNotifier = 
                new KafkaFactoryModelChangeNotifier(factoryModel, kafkaTemplate);
            factoryModel.setNotifier(kafkaNotifier);
            
            logger.info("Configured Kafka notifier for factory '{}' with topic '{}'", 
                       factoryId, kafkaNotifier.getTopicName());
            
            // Create a simulator controller for this factory
            SimulatorController simulator = new SimulatorController(factoryModel, persistenceManager);
            
            // Store the simulator in our running simulations map
            runningSimulators.put(factoryId, simulator);
            
            // DON'T START ANIMATION HERE - let the GUI window do it after it's created
            // This prevents threading issues
            
            logger.info("!!! ABOUT TO CREATE GUI WINDOW FOR: {}", factoryId);
            
            // Open GUI window for this simulation on the Swing Event Dispatch Thread
            final SimulatorController finalSimulator = simulator;
            final String finalFactoryId = factoryId;
            
            logger.info("!!! CALLING SwingUtilities.invokeLater NOW");
            
            // ONLY ONE approach - just use invokeLater (removed duplicate thread)
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    createGUIWindow(finalSimulator, finalFactoryId);
                }
            });
            
            logger.info("!!! SwingUtilities.invokeLater called - GUI window will appear");
            
            logger.info("Successfully started simulation for factory ID: {}", factoryId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error starting simulation for factory ID {}: {}", factoryId, e.getMessage(), e);
            // Clean up in case of error
            runningSimulators.remove(factoryId);
            return false;
        }
    }
    
    /**
     * Retrieves the factory model currently being simulated.
     * 
     * @param factoryId The ID of the factory to retrieve
     * @return The factory model if simulation is running, null otherwise
     */
    public Factory getSimulatedFactory(String factoryId) {
        logger.debug("Retrieving simulated factory with ID: {}", factoryId);
        
        SimulatorController simulator = runningSimulators.get(factoryId);
        if (simulator == null) {
            logger.warn("No running simulation found for factory ID: {}", factoryId);
            return null;
        }
        
        return (Factory) simulator.getCanvas();
    }
    
    /**
     * Stops the simulation for the specified factory ID.
     * 
     * @param factoryId The ID of the factory to stop simulating
     * @return true if the simulation was stopped successfully, false otherwise
     */
    public boolean stopSimulation(String factoryId) {
        logger.info("Attempting to stop simulation for factory ID: {}", factoryId);
        
        SimulatorController simulator = runningSimulators.get(factoryId);
        if (simulator == null) {
            logger.warn("No running simulation found for factory ID: {}", factoryId);
            return false;
        }
        
        try {
            // Stop the animation/simulation
            simulator.stopAnimation();
            
            // Remove from running simulations
            runningSimulators.remove(factoryId);
            
            logger.info("Successfully stopped simulation for factory ID: {}", factoryId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error stopping simulation for factory ID {}: {}", factoryId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Checks if a simulation is currently running for the specified factory ID.
     * 
     * @param factoryId The ID of the factory to check
     * @return true if simulation is running, false otherwise
     */
    public boolean isSimulationRunning(String factoryId) {
        SimulatorController simulator = runningSimulators.get(factoryId);
        return simulator != null && simulator.isAnimationRunning();
    }
    
    /**
     * Gets the set of all currently running simulation IDs.
     * 
     * @return Set of factory IDs that are currently being simulated
     */
    public java.util.Set<String> getRunningSimulationIds() {
        return runningSimulators.keySet();
    }
    
    /**
     * Loads a factory model from the persistence web server.
     * This method calls the persistence web server on port 8888 to read the factory model.
     * 
     * @param factoryId The ID of the factory to load
     * @return The loaded factory model or null if not found/error occurred
     */
    private Factory loadFactoryModel(String factoryId) {
        // Try local file system FIRST with absolute path
        try {
            logger.info("Attempting to load from local file system for ID: {}", factoryId);
            
            // Build absolute path to factory file
            String baseDir = System.getProperty("user.dir");
            java.io.File factoryDir = new java.io.File(baseDir, localFactoryDirectory).getCanonicalFile();
            java.io.File factoryFile = new java.io.File(factoryDir, factoryId);
            
            logger.info("Looking for factory file at: {}", factoryFile.getAbsolutePath());
            
            if (!factoryFile.exists()) {
                // Try fallback location
                factoryFile = new java.io.File("D:\\Users\\Camil\\Downloads\\robotic-factory-simulator\\fr.tp.inf112.projects.robotsim", factoryId);
                logger.info("Trying fallback location: {}", factoryFile.getAbsolutePath());
            }
            
            if (factoryFile.exists()) {
                // CRITICAL FIX: Pass the ABSOLUTE PATH to read(), not just the factoryId
                logger.info("File exists, reading with absolute path: {}", factoryFile.getAbsolutePath());
                Factory factory = (Factory) persistenceManager.read(factoryFile.getAbsolutePath());
                if (factory != null) {
                    logger.info("Successfully loaded factory from: {}", factoryFile.getAbsolutePath());
                    return factory;
                }
            } else {
                logger.warn("Factory file does not exist: {}", factoryFile.getAbsolutePath());
            }
        } catch (Exception ex) {
            logger.warn("Local file load failed: {}", ex.getMessage());
        }
        
        // Fallback to persistence server if local load failed
        try {
            logger.info("Calling persistence web server to load factory model with ID: {}", factoryId);
            
            String url = persistenceServerUrl + "/factory/" + factoryId;
            logger.debug("Persistence server URL: {}", url);
            
            Factory factory = restTemplate.getForObject(url, Factory.class);
            
            if (factory != null) {
                logger.info("Successfully loaded factory model from persistence server with ID: {}", factoryId);
                return factory;
            } else {
                logger.warn("Persistence server returned null for factory ID: {}", factoryId);
            }
            
        } catch (Exception e) {
            logger.error("Error calling persistence server for factory ID {}: {}", factoryId, e.getMessage());
        }
        
        return null;
    }

    /**
     * Helper method to create GUI window for a factory simulation
     */
    private void createGUIWindow(SimulatorController simulator, String factoryId) {
        try {
            logger.info("!!! INSIDE createGUIWindow - Creating GUI for: {}", factoryId);
            
            // Get the factory model to start simulation directly
            Factory factory = (Factory) simulator.getCanvas();
            
            // Create canvas chooser for this factory
            FileCanvasChooser canvasChooser = new FileCanvasChooser("factory", "Factory: " + factoryId);
            logger.info("!!! FileCanvasChooser created");
            
            // CRITICAL: CanvasViewer creates its OWN JFrame window, don't wrap it in another frame!
            Component factoryViewer = new CanvasViewer(simulator);
            logger.info("!!! CanvasViewer created (with its own window)");
            
            // Link the viewer to the chooser - this makes the CanvasViewer's window appear
            canvasChooser.setViewer(factoryViewer);
            logger.info("!!! Viewer linked to chooser - window should be visible now");
            
            // CRITICAL FIX: Find the JFrame that CanvasViewer created and change its close behavior
            SwingUtilities.invokeLater(() -> {
                // CanvasViewer is a Component, find its parent JFrame
                java.awt.Window window = SwingUtilities.getWindowAncestor(factoryViewer);
                if (window instanceof JFrame) {
                    JFrame frame = (JFrame) window;
                    logger.info("!!! Found CanvasViewer's JFrame, setting close operation");
                    
                    // Change close operation to not exit the app
                    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    
                    // Add our custom window listener
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            logger.info("!!! Window closing event for factory: {}", factoryId);
                            try {
                                // Stop the simulation
                                logger.info("!!! Calling stopSimulation for: {}", factoryId);
                                boolean stopped = stopSimulation(factoryId);
                                logger.info("!!! Simulation stopped: {} for factory: {}", stopped, factoryId);
                                
                                // Dispose the frame WITHOUT exiting the application
                                logger.info("!!! Disposing frame for: {}", factoryId);
                                frame.setVisible(false);
                                frame.dispose();
                                logger.info("!!! Frame disposed for: {} - SERVICE STILL RUNNING", factoryId);
                            } catch (Exception ex) {
                                logger.error("!!! Error in window closing handler: {}", ex.getMessage(), ex);
                            }
                        }
                    });
                    
                    logger.info("!!! Window close handler installed - closing window will NOT kill service");
                }
            });
            
            // START ANIMATION IMMEDIATELY
            logger.info("!!! Starting simulation on factory");
            factory.startSimulation();
            logger.info("!!! Factory.startSimulation() called - state: {}", factory.isSimulationStarted());
            
            // Also call via controller
            simulator.startAnimation();
            logger.info("!!! Controller.startAnimation() called");
            
            // Force start animation again after a brief delay to ensure it's running
            SwingUtilities.invokeLater(() -> {
                logger.info("!!! POST-CREATE: Ensuring animation is running for {}", factoryId);
                if (!factory.isSimulationStarted()) {
                    logger.warn("!!! Simulation wasn't running, starting it now!");
                    factory.startSimulation();
                }
                simulator.startAnimation();
                logger.info("!!! POST-CREATE: Animation state: {}", factory.isSimulationStarted());
            });
            
            logger.info("!!! GUI window created and animation started for: {}", factoryId);
            
        } catch (Exception e) {
            logger.error("!!! ERROR creating GUI window: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
