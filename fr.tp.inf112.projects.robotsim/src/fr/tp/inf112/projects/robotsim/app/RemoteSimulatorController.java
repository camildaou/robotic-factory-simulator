package fr.tp.inf112.projects.robotsim.app;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.Component;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.LocalFactoryModelChangedNotifier;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;

/**
 * Remote simulator controller that communicates with a simulation microservice
 * instead of directly controlling a local Factory object. This controller makes
 * HTTP REST calls to start/stop simulation and periodically updates the viewer
 * with fresh model data from the microservice.
 */
public class RemoteSimulatorController extends SimulatorController {
    
    private static final Logger LOGGER = Logger.getLogger(RemoteSimulatorController.class.getName());
    
    // Microservice connection settings
    private final String microserviceHost;
    private final int microservicePort;
    private final String factoryId;
    
    // HTTP client for REST API calls
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // Background thread management
    private final ExecutorService executorService;
    private CompletableFuture<Void> updateViewerTask;
    private volatile boolean isUpdatingViewer = false;
    
    // Local registry of observers that have been added to this controller. We
    // keep our own list because Canvas/Factory doesn't expose a getObservers()
    // method publicly. Observers added through this controller will be
    // forwarded to the active canvas model when present.
    private final List<Observer> localObservers = new java.util.concurrent.CopyOnWriteArrayList<>();
    
    // Local notifier for notifying observers of model changes
    private final LocalFactoryModelChangedNotifier localNotifier = new LocalFactoryModelChangedNotifier();
    
    /**
     * Creates a new RemoteSimulatorController.
     * 
     * @param factoryModel Initial factory model (can be null, will be fetched from microservice)
     * @param persistenceManager Persistence manager for saving/loading factory models
     * @param microserviceHost Host address of the simulation microservice (e.g., "localhost")
     * @param microservicePort Port of the simulation microservice (e.g., 8080)
     * @param factoryId Unique identifier for the factory in the microservice
     */
    public RemoteSimulatorController(final Factory factoryModel,
                                   final CanvasPersistenceManager persistenceManager,
                                   final String microserviceHost,
                                   final int microservicePort,
                                   final String factoryId) {
        super(factoryModel, persistenceManager);
        
        this.microserviceHost = microserviceHost;
        this.microservicePort = microservicePort;
        this.factoryId = factoryId;
        
        // Initialize HTTP client
        this.httpClient = HttpClient.newHttpClient();
        
        // Initialize Jackson ObjectMapper with polymorphic type support
        this.objectMapper = new ObjectMapper();
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(PositionedShape.class.getPackageName())
            .allowIfSubType(Component.class.getPackageName())
            .build();
        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
        
        // Initialize thread pool for background tasks
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "RemoteSimulator-ViewerUpdater");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Convenience constructor with default localhost settings.
     */
    public RemoteSimulatorController(final Factory factoryModel,
                                   final CanvasPersistenceManager persistenceManager,
                                   final String factoryId) {
        this(factoryModel, persistenceManager, "localhost", 8090, factoryId);
    }
    
    /**
     * {@inheritDoc}
     * 
     * Overridden to start simulation via microservice and begin periodic viewer updates.
     */
    @Override
    public void startAnimation() {
        try {
            // Call microservice to start simulation
            final URI uri = new URI("http", null, microserviceHost, microservicePort, 
                                  "/api/simulation/start/" + factoryId, null, null);
            
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();
            
            final HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LOGGER.info("Simulation started successfully via microservice");
                
                // Start background viewer update process
                startViewerUpdates();
            } else {
                LOGGER.severe("Failed to start simulation. HTTP status: " + response.statusCode() + 
                            ", Response: " + response.body());
            }
            
        } catch (URISyntaxException | IOException | InterruptedException e) {
            LOGGER.severe("Error starting simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * Overridden to stop simulation via microservice and halt viewer updates.
     */
    @Override
    public void stopAnimation() {
        try {
            // Stop background viewer updates first
            stopViewerUpdates();
            
            // Call microservice to stop simulation
            final URI uri = new URI("http", null, microserviceHost, microservicePort, 
                                  "/api/simulation/stop/" + factoryId, null, null);
            
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();
            
            final HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LOGGER.info("Simulation stopped successfully via microservice");
            } else {
                LOGGER.severe("Failed to stop simulation. HTTP status: " + response.statusCode() + 
                            ", Response: " + response.body());
            }
            
        } catch (URISyntaxException | IOException | InterruptedException e) {
            LOGGER.severe("Error stopping simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * Checks simulation status via microservice.
     */
    @Override
    public boolean isAnimationRunning() {
        try {
            // Use the /running endpoint to check if this factory is in the running list
            final URI uri = new URI("http", null, microserviceHost, microservicePort, 
                                  "/api/simulation/running", null, null);
            
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();
            
            final HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Check if our factoryId is in the running simulations list
                return response.body().contains("\"" + factoryId + "\"");
            }
            
        } catch (URISyntaxException | IOException | InterruptedException e) {
            LOGGER.warning("Error checking simulation status: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Fetches the current factory model from the microservice.
     * 
     * @return Factory model from microservice, or null if error occurred
     */
    public Factory getFactory() {
        try {
            final URI uri = new URI("http", null, microserviceHost, microservicePort, 
                                  "/api/simulation/factory/" + factoryId, null, null);
            
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();
            
            final HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parse JSON response to Factory object
                final Factory factory = objectMapper.readValue(response.body(), Factory.class);
                LOGGER.fine("Successfully retrieved factory model from microservice");
                return factory;
            } else {
                LOGGER.warning("Failed to retrieve factory model. HTTP status: " + response.statusCode());
            }
            
        } catch (URISyntaxException | IOException | InterruptedException e) {
            LOGGER.warning("Error retrieving factory model: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Starts the background process that periodically updates the viewer
     * with fresh factory model data from the microservice.
     */
    private void startViewerUpdates() {
        if (!isUpdatingViewer) {
            isUpdatingViewer = true;
            updateViewerTask = CompletableFuture.runAsync(this::updateViewer, executorService);
        }
    }
    
    /**
     * Stops the background viewer update process.
     */
    private void stopViewerUpdates() {
        isUpdatingViewer = false;
        if (updateViewerTask != null) {
            updateViewerTask.cancel(true);
        }
    }
    
    /**
     * Periodically queries the simulation microservice via Kafka to retrieve fresh factory model
     * and updates the viewer. This runs in a background thread while simulation is active.
     */
    private void updateViewer() {
        LOGGER.info("Starting Kafka event consumer for viewer updates");
        
        try {
            // Create and start Kafka consumer
            final FactorySimulationEventConsumer eventConsumer = 
                new FactorySimulationEventConsumer(this);
            
            // This blocks until simulation is stopped
            eventConsumer.consumeMessages();
            
        } catch (Exception e) {
            LOGGER.severe("Error in Kafka event consumer: " + e.getMessage());
            e.printStackTrace();
        }
        
        LOGGER.info("Kafka event consumer stopped");
    }
    
    /**
     * {@inheritDoc}
     * 
     * Overridden to properly handle observer management when switching to remote model.
     * This ensures that observers are preserved when the canvas model is updated.
     */
    @Override
    public void setCanvas(final Canvas canvasModel) {
        // Remove observers from previous canvas (if any) and attach local
        // observers to the new canvas via the public addObserver API.
        final Canvas previous = getCanvas();

        // Detach observers from previous canvas model
        if (previous instanceof fr.tp.inf112.projects.canvas.controller.Observable) {
            for (final Observer observer : localObservers) {
                // Use controller-level removeObserver which will forward to the
                // underlying model if present
                try {
                    removeObserver(observer);
                } catch (final Exception e) {
                    // Swallow to ensure robust transition
                }
            }
        }

        // Update the canvas model reference
        super.setCanvas(canvasModel);

        // Attach local observers to the new canvas model
        if (getCanvas() != null) {
            for (final Observer observer : localObservers) {
                try {
                    addObserver(observer);
                } catch (final Exception e) {
                    // Continue attaching remaining observers even if one fails
                }
            }
            
            // Note: Observers will be notified automatically when the model changes.
            // The Factory.notifyObservers() method is protected and not part of the
            // public Observable interface, so we don't call it directly here.
        }
    }

    /**
     * Override to keep local registry and forward to underlying model if present.
     */
    @Override
    public boolean addObserver(final Observer observer) {
        if (observer == null) {
            return false;
        }

        // Add to local notifier
        localNotifier.addObserver(observer);
        
        // Keep in local registry
        if (!localObservers.contains(observer)) {
            localObservers.add(observer);
        }

        // Forward to underlying canvas/factory if available
        final Canvas canvas = getCanvas();
        if (canvas instanceof fr.tp.inf112.projects.canvas.controller.Observable) {
            try {
                return ((fr.tp.inf112.projects.canvas.controller.Observable) canvas).addObserver(observer);
            } catch (final Exception e) {
                // Fall through and return true because it's in our registry
            }
        }

        return true;
    }

    /**
     * Override to keep local registry and forward removal to underlying model.
     */
    @Override
    public boolean removeObserver(final Observer observer) {
        if (observer == null) {
            return false;
        }

        // Remove from local notifier
        localNotifier.removeObserver(observer);
        
        // Remove from local registry
        localObservers.remove(observer);

        // Forward to underlying canvas/factory if available
        final Canvas canvas = getCanvas();
        if (canvas instanceof fr.tp.inf112.projects.canvas.controller.Observable) {
            try {
                return ((fr.tp.inf112.projects.canvas.controller.Observable) canvas).removeObserver(observer);
            } catch (final Exception e) {
                // Fall through and return true since it was removed locally
            }
        }

        return true;
    }
    
    /**
     * Parses JSON text into a Factory object and updates the canvas.
     * Also notifies local observers of the model change.
     * 
     * @param jsonText JSON representation of the Factory object
     */
    public void setCanvasFromJson(final String jsonText) {
        try {
            // Parse JSON into Factory object using ObjectMapper
            final Factory factory = objectMapper.readValue(jsonText, Factory.class);
            
            // Update the canvas
            setCanvas(factory);
            
            // Notify local observers
            localNotifier.notifyObservers();
            
            LOGGER.fine("Successfully updated canvas from JSON and notified observers");
            
        } catch (Exception e) {
            LOGGER.severe("Error parsing JSON factory text: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Shuts down the controller and releases resources.
     * Call this when the controller is no longer needed.
     */
    public void shutdown() {
        stopViewerUpdates();
        executorService.shutdown();
        LOGGER.info("RemoteSimulatorController shut down");
    }
    
    /**
     * Gets the microservice host address.
     * 
     * @return Microservice host
     */
    public String getMicroserviceHost() {
        return microserviceHost;
    }
    
    /**
     * Gets the microservice port.
     * 
     * @return Microservice port
     */
    public int getMicroservicePort() {
        return microservicePort;
    }
    
    /**
     * Gets the factory ID used in microservice calls.
     * 
     * @return Factory ID
     */
    public String getFactoryId() {
        return factoryId;
    }
}