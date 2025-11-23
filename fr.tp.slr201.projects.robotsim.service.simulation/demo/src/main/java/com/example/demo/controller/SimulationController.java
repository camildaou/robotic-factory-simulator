package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SimulationService;
import fr.tp.inf112.projects.robotsim.model.Factory;

/**
 * REST Controller for managing robot factory simulation services.
 * Provides endpoints to start, retrieve, and stop factory model simulations.
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);
    
    /**
     * Service for managing simulations
     */
    @Autowired
    private SimulationService simulationService;
    
    /**
     * Starts simulating a factory model identified by its ID.
     * 
     * @param factoryId The ID of the factory model to simulate
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/start/{factoryId}")
    public ResponseEntity<Boolean> startSimulation(@PathVariable String factoryId) {
        logger.info("Received request to start simulation for factory ID: {}", factoryId);
        
        try {
            // Check if simulation is already running
            if (simulationService.isSimulationRunning(factoryId)) {
                logger.warn("Simulation for factory ID {} is already running", factoryId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(false);
            }
            
            // Start the simulation using the service
            boolean success = simulationService.startSimulation(factoryId);
            
            if (success) {
                logger.info("Successfully started simulation for factory ID: {}", factoryId);
                return ResponseEntity.ok(true);
            } else {
                logger.error("Failed to start simulation for factory ID: {}", factoryId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
            }
            
        } catch (Exception e) {
            logger.error("Error starting simulation for factory ID {}: {}", factoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }
    
    /**
     * Retrieves a factory model currently being simulated by its ID.
     * Used by the factory viewer to obtain the simulated model state.
     * 
     * @param factoryId The ID of the factory model to retrieve
     * @return ResponseEntity with the factory model or error status
     */
    @GetMapping("/factory/{factoryId}")
    public ResponseEntity<Factory> getSimulatedFactory(@PathVariable String factoryId) {
        logger.info("Received request to retrieve simulated factory with ID: {}", factoryId);
        
        try {
            Factory factoryModel = simulationService.getSimulatedFactory(factoryId);
            
            if (factoryModel == null) {
                logger.warn("No running simulation found for factory ID: {}", factoryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            
            logger.debug("Successfully retrieved factory model for ID: {}", factoryId);
            return ResponseEntity.ok(factoryModel);
            
        } catch (Exception e) {
            logger.error("Error retrieving factory model for ID {}: {}", factoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Stops the simulation of a robotic factory model identified by its ID.
     * 
     * @param factoryId The ID of the factory model to stop simulating
     * @return ResponseEntity with success/failure status
     */
    @DeleteMapping("/stop/{factoryId}")
    public ResponseEntity<Boolean> stopSimulation(@PathVariable String factoryId) {
        logger.info("Received request to stop simulation for factory ID: {}", factoryId);
        
        try {
            boolean success = simulationService.stopSimulation(factoryId);
            
            if (success) {
                logger.info("Successfully stopped simulation for factory ID: {}", factoryId);
                return ResponseEntity.ok(true);
            } else {
                logger.warn("No running simulation found for factory ID: {}", factoryId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
            }
            
        } catch (Exception e) {
            logger.error("Error stopping simulation for factory ID {}: {}", factoryId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }
    
    /**
     * Gets the list of all currently running simulation IDs.
     * 
     * @return ResponseEntity with the set of running simulation IDs
     */
    @GetMapping("/running")
    public ResponseEntity<java.util.Set<String>> getRunningSimulations() {
        logger.info("Received request to retrieve list of running simulations");
        
        try {
            java.util.Set<String> runningIds = simulationService.getRunningSimulationIds();
            logger.debug("Found {} running simulations", runningIds.size());
            return ResponseEntity.ok(runningIds);
            
        } catch (Exception e) {
            logger.error("Error retrieving running simulations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Collections.emptySet());
        }
    }
    
    /**
     * Health check endpoint to verify the service is running.
     * 
     * @return ResponseEntity with service status
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.debug("Health check requested");
        return ResponseEntity.ok("Simulation service is running");
    }
}