# Robot Factory Simulation Microservice

A Spring Boot microservice for managing robot factory simulations. This service provides REST endpoints to start, retrieve, and stop factory model simulations.

## Features

- **Start Simulation**: Initialize and start simulation for a factory model by ID
- **Retrieve Factory Model**: Get the current state of a simulated factory model
- **Stop Simulation**: Halt and cleanup simulation for a factory model
- **Health Check**: Monitor service availability
- **Comprehensive Logging**: Detailed logging for monitoring and debugging

## API Endpoints

### Start Simulation
```
POST /api/simulation/start/{factoryId}
```
Starts simulating a factory model identified by its ID.

**Response:**
- `200 OK`: Returns `true` if simulation started successfully
- `409 Conflict`: Returns `false` if simulation is already running
- `400 Bad Request`: Returns `false` if failed to start simulation
- `500 Internal Server Error`: Returns `false` for unexpected errors

**Example:**
```bash
curl -X POST http://localhost:8090/api/simulation/start/factory123
```

### Retrieve Simulated Factory
```
GET /api/simulation/factory/{factoryId}
```
Retrieves a factory model currently being simulated by its ID. Used by factory viewers to obtain the fresh model state.

**Response:**
- `200 OK`: Returns the Factory object in JSON format
- `404 Not Found`: No running simulation found for the given ID
- `500 Internal Server Error`: Unexpected error occurred

**Example:**
```bash
curl http://localhost:8090/api/simulation/factory/factory123
```

### Stop Simulation
```
DELETE /api/simulation/stop/{factoryId}
```
Stops the simulation of a robotic factory model identified by its ID.

**Response:**
- `200 OK`: Returns `true` if simulation stopped successfully
- `404 Not Found`: Returns `false` if no simulation found for the given ID
- `500 Internal Server Error`: Returns `false` for unexpected errors

**Example:**
```bash
curl -X DELETE http://localhost:8090/api/simulation/stop/factory123
```

### Get Running Simulations
```
GET /api/simulation/running
```
Gets the list of all currently running simulation IDs.

**Response:**
- `200 OK`: Returns a set of factory IDs currently being simulated
- `500 Internal Server Error`: Returns empty set for unexpected errors

**Example:**
```bash
curl http://localhost:8090/api/simulation/running
```

### Health Check
```
GET /api/simulation/health
```
Health check endpoint to verify the service is running.

**Response:**
- `200 OK`: Returns "Simulation service is running"

**Example:**
```bash
curl http://localhost:8090/api/simulation/health
```

## Configuration

The service can be configured through `application.properties`:

```properties
# Server configuration
server.port=8090

# Logging levels
logging.level.com.example.demo=INFO
logging.level.fr.tp.inf112.projects.robotsim=DEBUG

# Log file configuration
logging.file.name=logs/simulation-service.log
```

## Building and Running

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- The robotsim library and dependencies

### Build
```bash
cd demo
mvn clean compile
```

### Run
```bash
mvn spring-boot:run
```

The service will start on port 8090 by default.

## Architecture

### Components

1. **SimulationController**: REST controller handling HTTP requests
2. **SimulationService**: Business logic for managing simulations  
3. **Factory/SimulatorController**: Core simulation engine from the robotsim library
4. **FactoryPersistenceManager**: Handles loading/saving factory models

### Integration with Robotsim Library

This microservice integrates with the existing robotsim simulation framework:

- Uses `Factory` models from `fr.tp.inf112.projects.robotsim.model`
- Leverages `SimulatorController` from `fr.tp.inf112.projects.robotsim.app`
- Utilizes `FactoryPersistenceManager` for model persistence
- Supports the complete robotsim simulation lifecycle

## Logging

The service provides comprehensive logging for monitoring and debugging:

- **INFO level**: API requests, simulation lifecycle events
- **DEBUG level**: Detailed operation traces  
- **WARN level**: Non-critical issues
- **ERROR level**: Failures and exceptions

Logs are written to both console and `logs/simulation-service.log` file.

## Error Handling

All endpoints provide proper error handling with:
- Appropriate HTTP status codes
- Descriptive error logging
- Graceful degradation for failures
- Resource cleanup on errors

## Future Enhancements

- Integration with external persistence web server
- Metrics and monitoring endpoints
- Configuration management for persistence server URLs
- WebSocket support for real-time simulation updates
- Authentication and authorization