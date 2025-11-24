# Robot Factory Simulator - Distributed System

A comprehensive distributed factory simulation system demonstrating microservices architecture, event-driven communication with Apache Kafka, and real-time visualization of autonomous robots navigating through factory environments.

## � Getting Started - Opening the Project

This project is ready to open in **IntelliJ IDEA** or **Eclipse** with all configuration files included:

**IMPORTANT**: This repository contains **two separate projects**. You must open each project folder individually, NOT the root folder.

### IntelliJ IDEA (Recommended)
1. **Clone the repository**: `git clone https://github.com/camildaou/robotic-factory-simulator.git`
2. **Open Main Project**: File → Open → Select `robotic-factory-simulator/fr.tp.inf112.projects.robotsim`
3. **Open Microservice** (in new window): File → Open → Select `robotic-factory-simulator/fr.tp.slr201.projects.robotsim.service.simulation` → Choose **New Window**
4. **Run configurations will appear**: `SimulatorApplication`, `RemoteSimulatorApplication`, `FactoryPersistenceServer`, `DemoApplication`

### Eclipse
1. **Clone the repository**: `git clone https://github.com/camildaou/robotic-factory-simulator.git`
2. **Import Projects**: File → Import → Existing Projects into Workspace
3. **Select root directory**: Browse to `robotic-factory-simulator`
4. **Select both projects** and click **Finish**

**Note**: DO NOT open the root `robotic-factory-simulator` folder in IntelliJ - open each subproject separately!

---

## �📋 Table of Contents

- [Project Overview](#project-overview)
- [System Architecture](#system-architecture)
- [Prerequisites](#prerequisites)
- [Quick Start Guide](#quick-start-guide)
- [Detailed Setup Instructions](#detailed-setup-instructions)
  - [1. Running the Persistence Server](#1-running-the-persistence-server)
  - [2. Starting Apache Kafka](#2-starting-apache-kafka)
  - [3. Running the Simulation Microservice](#3-running-the-simulation-microservice)
  - [4. Running the Monolithic Simulator Application](#4-running-the-monolithic-simulator-application)
  - [5. Running the Remote Simulator Application](#5-running-the-remote-simulator-application)
- [Testing the System](#testing-the-system)
  - [Running JUnit Tests](#running-junit-tests)
  - [Testing REST API Endpoints](#testing-rest-api-endpoints)
  - [End-to-End Integration Testing](#end-to-end-integration-testing)
- [Features](#features)
- [Technologies Used](#technologies-used)

---

## 📖 Project Overview

This project implements a distributed robot factory simulation system where autonomous robots navigate through factory floors, avoiding obstacles and reaching target destinations. The system demonstrates:

- **Microservices Architecture**: Separation of concerns with independent simulation and persistence services
- **Event-Driven Communication**: Real-time updates using Apache Kafka message streaming
- **Distributed Persistence**: Socket-based and HTTP REST persistence layers
- **Real-Time Visualization**: Multiple GUI applications showing live factory simulations
- **Path Finding Algorithms**: Custom Dijkstra and JGraphT implementations for robot navigation
- **Serialization**: Jackson JSON and Java native serialization with proper handling of bidirectional references

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Interfaces                          │
├────────────────┬────────────────────┬──────────────────────────┤
│   Simulator    │  Remote Simulator  │  REST API Endpoints      │
│   Application  │   Application      │  (Postman/HTTP Client)   │
└────────┬───────┴──────────┬─────────┴──────────────┬───────────┘
         │                  │                        │
         │                  └────────────────────────┼─────────┐
         │                                           │         │
    ┌────▼──────────────────────────────────────────▼──────┐  │
    │         Simulation Microservice (Port 8090)          │  │
    │  - Factory Model Management                          │  │
    │  - REST API Controllers                              │  │
    │  - Kafka Event Publishing                            │  │
    │  - Automatic GUI Window Creation                     │  │
    └────────────────┬──────────────────┬──────────────────┘  │
                     │                  │                      │
              ┌──────▼─────┐     ┌─────▼────────┐            │
              │   Apache   │     │  Persistence │            │
              │   Kafka    │     │    Server    │            │
              │ (Port 9092)│     │ (Port 8888)  │            │
              └────────────┘     └──────────────┘            │
                     │                                        │
                     └────────────────────────────────────────┘
                               (Kafka Consumer)
```

---

## ✅ Prerequisites

Before running the project, ensure you have the following installed:

1. **Java Development Kit (JDK)**: Version 17 or higher
   - Check: `java -version`

2. **IntelliJ IDEA**: Ultimate or Community Edition
   - Download from: https://www.jetbrains.com/idea/download/

3. **Apache Maven**: Version 3.6 or higher
   - Check: `mvn -version`
   - Usually bundled with IntelliJ IDEA

4. **Docker Desktop**: For running Apache Kafka
   - Download from: https://www.docker.com/products/docker-desktop
   - Check: `docker --version`

5. **Git**: For version control (optional)
   - Check: `git --version`

---

## 🚀 Quick Start Guide

For a quick test of the entire system:

1. **Start Kafka**: `docker-compose up -d`
2. **Start Simulation Service**: Run `DemoApplication` in IntelliJ
3. **Test API**: POST to `http://localhost:8090/api/simulation/start/factory123.factory`
4. **View Animation**: Automatic GUI window opens with live simulation

For detailed instructions, see sections below.

---

## 📚 Detailed Setup Instructions

### 1. Running the Persistence Server

The persistence server handles saving and loading factory models using socket-based communication.

#### Steps in IntelliJ IDEA:

1. **Open Project**:
   - File → Open → Select `fr.tp.inf112.projects.robotsim` folder

2. **Locate Main Class**:
   - Navigate to: `src/fr/tp/inf112/projects/robotsim/model/FactoryPersistenceServer.java`

3. **Run the Server**:
   - Right-click on `FactoryPersistenceServer.java`
   - Select **Run 'FactoryPersistenceServer.main()'**

4. **Verify Server Started**:
   ```
   Console Output:
   ✓ Server started on port 8888
   ✓ Waiting for client connections...
   ```

5. **Keep Running**: Leave this server running in the background

---

### 2. Starting Apache Kafka

Apache Kafka provides event streaming for real-time factory simulation updates.

#### Steps:

1. **Navigate to Project Root**:
   ```powershell
   cd d:\Users\Camil\Downloads\robotic-factory-simulator
   ```

2. **Start Kafka and Zookeeper**:
   ```powershell
   docker-compose up -d
   ```

3. **Verify Containers Running**:
   ```powershell
   docker ps
   ```
   
   Expected output:
   ```
   CONTAINER ID   IMAGE                             STATUS
   <id>           confluentinc/cp-kafka:7.5.0      Up
   <id>           confluentinc/cp-zookeeper:7.5.0  Up
   ```

4. **Check Kafka Logs** (optional):
   ```powershell
   docker logs kafka
   ```

5. **Stop Kafka** (when done):
   ```powershell
   docker-compose down
   ```

---

### 3. Running the Simulation Microservice

The microservice manages factory simulations and provides REST API endpoints.

#### Steps in IntelliJ IDEA:

1. **Open Microservice Project**:
   - File → Open → Select `fr.tp.slr201.projects.robotsim.service.simulation/demo` folder

2. **Configure JVM Arguments** (Important for GUI):
   - Run → Edit Configurations
   - Select **DemoApplication** (or create new Spring Boot configuration)
   - In **VM Options** field, add:
     ```
     -Djava.awt.headless=false
     ```
   - Click **Apply** → **OK**

3. **Run the Application**:
   - Click the **Run** button (green play icon)
   - Or right-click `DemoApplication.java` → **Run 'DemoApplication'**

   PLEASE NOTE THAT: when running the simulation microservice with gui, there is a bug i was not able to fix where closing the animation tab leads to  the killing of the service. I believe this is related to the functionality of canvas viewer and was not able to fix it.

4. **Verify Startup**:
   ```
   Console Output:
   ✓ Started DemoApplication in X.XXX seconds
   ✓ Tomcat started on port 8090
   ✓ Graphics environment available - GUI windows enabled
   ✓ AWT Event Dispatch Thread initialized and running
   ✓ Configured Kafka producer
   ```

5. **Microservice Endpoints**:
   - Base URL: `http://localhost:8090`
   - Health Check: `GET http://localhost:8090/api/simulation/health`

---

### 4. Running the Monolithic Simulator Application

Traditional standalone application with GUI for designing and simulating factories.

#### Steps in IntelliJ IDEA:

1. **Open Project**: `fr.tp.inf112.projects.robotsim`

2. **Locate Main Class**:
   - Navigate to: `src/fr/tp/inf112/projects/robotsim/app/SimulatorApplication.java`

3. **Run the Application**:
   - Right-click on `SimulatorApplication.java`
   - Select **Run 'SimulatorApplication.main()'**

4. **GUI Window Opens**:
   - Factory viewer window appears
   - Menu bar: File → Open, Save, New
   - Toolbar: Start/Stop simulation buttons

5. **Using the Application**:
   - **Load Factory**: File → Open → Select `.factory` file
   - **Start Simulation**: Click **Start Animation** button
   - **Save Factory**: File → Save As → Enter filename
   - **Design Factory**: Add rooms, robots, machines using design tools

---

### 5. Running the Remote Simulator Application

Connects to the simulation microservice via Kafka for distributed viewing.

#### Steps in IntelliJ IDEA:

1. **Prerequisites**: Ensure Kafka and Simulation Microservice are running

2. **Locate Main Class**:
   - Navigate to: `src/fr/tp/inf112/projects/robotsim/app/RemoteSimulatorApplication.java`

3. **Run with Arguments** (optional):
   - Right-click → **Modify Run Configuration**
   - **Program Arguments**: `localhost 8888` (persistence server host and port)
   - Click **OK**

4. **Run the Application**:
   - Click **Run** button
   - Or: Right-click → **Run 'RemoteSimulatorApplication.main()'**

5. **GUI Window Opens**:
   - Remote viewer connects to microservice
   - Displays factory simulation from remote source
   - Updates in real-time via Kafka events

6. **Verify Connection**:
   ```
   Console Output:
   ✓ Remote simulator started
   ✓ Connected to server: localhost:8888
   ✓ Starting Kafka event consumer for viewer updates
   ```

---

## 🧪 Testing the System

### Running JUnit Tests

The project includes comprehensive unit tests for serialization, deserialization, and model integrity.

#### Steps in IntelliJ IDEA:

1. **Open Test Package**:
   - Navigate to: `fr.tp.inf112.projects.robotsim/src/test/java`

2. **Run All Tests**:
   - Right-click on `test` folder
   - Select **Run 'All Tests'**

3. **Run Specific Test Class**:
   - Navigate to: `\fr.tp.inf112.projects.robotsim\src\test\java\fr\tp\inf112\projects\robotsim\model\TestRobotSimSerializationJSON.java`
   - Right-click → **Run 'TestRobotSimSerializationJSON'**

4. **Verify Results**:
   ```
   Test Results Window:
   ✓ All tests passed
   ✓ Jackson serialization tests passed
   ✓ Bidirectional reference tests passed
   ✓ Polymorphic type handling tests passed
   ```

5. **View Coverage** (optional):
   - Right-click test folder → **Run with Coverage**
   - Coverage report shows tested code paths



### Testing REST API Endpoints

Test the simulation microservice REST API using IntelliJ's HTTP Client.

#### Method 1: Using IntelliJ HTTP Client

1. **Open HTTP Request File**:
   - Navigate to: `demo/test-endpoints.http`

2. **Execute Requests**:
   - Click green **play icon** next to each request
   - Or: **Ctrl + Enter** (Windows/Linux) / **Cmd + Enter** (Mac)

3. **Example Requests**:

   ```http
   ### Health Check
   GET http://localhost:8090/api/simulation/health
   
   ### Start Simulation
   POST http://localhost:8090/api/simulation/start/factory123.factory
   
   ### Get Factory Status
   GET http://localhost:8090/api/simulation/factory/factory123.factory
   
   ### Get Running Simulations
   GET http://localhost:8090/api/simulation/running
   
   ### Stop Simulation
   DELETE http://localhost:8090/api/simulation/stop/factory123.factory
   ```

4. **View Responses**:
   - Responses appear in bottom panel
   - Status codes, headers, and body visible

#### Method 2: Using PowerShell

```powershell
# Health Check
Invoke-RestMethod -Uri "http://localhost:8090/api/simulation/health" -Method GET

# Start Simulation
Invoke-RestMethod -Uri "http://localhost:8090/api/simulation/start/factory123.factory" -Method POST

# Get Running Simulations
Invoke-RestMethod -Uri "http://localhost:8090/api/simulation/running" -Method GET

# Stop Simulation
Invoke-RestMethod -Uri "http://localhost:8090/api/simulation/stop/factory123.factory" -Method DELETE
```

#### Method 3: Using Postman

1. **Import Collection**: Create new collection
2. **Add Requests**: Use endpoints listed above
3. **Set Base URL**: `http://localhost:8090`
4. **Send Requests**: Click **Send** button

---

### End-to-End Integration Testing

Test the complete distributed system workflow.

#### Complete Test Scenario:

**Step 1: Start All Services**

```powershell
# Terminal 1: Start Kafka
docker-compose up -d

# Terminal 2: Start Persistence Server (in IntelliJ)
# Run FactoryPersistenceServer.main()

# Terminal 3: Start Simulation Microservice (in IntelliJ)
# Run DemoApplication
```

**Step 2: Create/Save Factory**

1. Run `RemoteSimulatorApplication`
2. Design factory or open existing: `factory123.factory`
3. Save factory: File → Save
4. Note the factory filename

**Step 3: Start Simulation via API**

```powershell
# PowerShell
Invoke-RestMethod -Uri "http://localhost:8090/api/simulation/start/factory123.factory" -Method POST
```

**Expected Result:**
- ✅ GUI window opens automatically
- ✅ Factory visualization displays
- ✅ Robots begin moving with smooth animation
- ✅ Console shows: "Successfully started simulation"

**Step 4: Monitor Kafka Events**

```powershell
# Monitor Kafka topic
docker exec -it kafka kafka-console-consumer `
  --bootstrap-server localhost:9092 `
  --topic simulation-factory123.factory `
  --from-beginning
```

**Expected Output:**
```json
{"id":"factory123.factory","width":200,"height":200,"components":[...]}
{"id":"factory123.factory","width":200,"height":200,"components":[...]}
...
```

**Step 5: Test Remote Viewer**

1. Run `RemoteSimulatorApplication` in IntelliJ
2. Window opens showing same simulation
3. Verify real-time updates via Kafka

**Step 6: Stop Simulation**

```powershell
Invoke-RestMethod -Uri "http://localhost:8090/api/simulation/stop/factory123.factory" -Method DELETE
```

**Expected Result:**
- ✅ Animation stops
- ✅ Microservice continues running
- ✅ Can start another simulation

**Step 7: Verify State**

```powershell
# Check running simulations
Invoke-RestMethod -Uri "http://localhost:8090/api/simulation/running" -Method GET
```

Expected: Empty list `[]`

---

## ✨ Features


### Architecture Features

- **Microservices Architecture**: Independent, scalable services
- **Event-Driven Design**: Kafka-based asynchronous communication
- **RESTful API**: Standard HTTP endpoints for integration
- **Distributed Persistence**: Multiple persistence strategies
- **Real-Time Updates**: Sub-second visualization updates
- **Multi-Instance Support**: Multiple simulations simultaneously

### Technical Features

- **Jackson JSON Serialization**: Proper handling of:
  - Bidirectional references with `@JsonManagedReference`/`@JsonBackReference`
  - Polymorphic types with `@JsonTypeInfo`
  - Custom deserializers for complex objects
- **Thread-Safe Operations**: Synchronized component movements
- **Observer Pattern**: Model-View separation
- **Configurable Notifiers**: Local vs. Kafka notification strategies

---

## 🛠️ Technologies Used

### Backend

- **Java 17**: Core programming language
- **Spring Boot 3.5.6**: Microservice framework
- **Spring Kafka 3.3.10**: Kafka integration
- **Apache Kafka 7.5.0**: Event streaming platform
- **Jackson 2.19.2**: JSON serialization/deserialization
- **Maven**: Dependency management and build tool

### Frontend

- **Java Swing**: GUI framework
- **AWT**: Graphics and window management
- **Custom Canvas Framework**: Factory visualization

### Libraries

- **JGraphT 1.5.2**: Graph algorithms and path finding
- **SLF4J + Logback**: Logging framework
- **JUnit 5**: Unit testing framework

### Infrastructure

- **Docker**: Containerization for Kafka
- **Docker Compose**: Multi-container orchestration
- **Zookeeper**: Kafka cluster coordination

---

## 📝 Project Structure

```
robotic-factory-simulator/
├── fr.tp.inf112.projects.robotsim/          # Main simulator project
│   ├── src/
│   │   ├── fr/tp/inf112/projects/robotsim/
│   │   │   ├── app/                         # Application entry points
│   │   │   │   ├── SimulatorApplication.java
│   │   │   │   ├── RemoteSimulatorApplication.java
│   │   │   │   ├── SimulatorController.java
│   │   │   │   └── RemoteSimulatorController.java
│   │   │   ├── model/                       # Domain models
│   │   │   │   ├── Factory.java
│   │   │   │   ├── Robot.java
│   │   │   │   ├── Component.java
│   │   │   │   ├── FactoryPersistenceManager.java
│   │   │   │   └── FactoryPersistenceServer.java
│   │   │   └── view/                        # GUI components
│   │   └── test/                            # JUnit tests
│   ├── *.factory                            # Factory model files
│   └── libs/                                # JGraphT library
│
├── fr.tp.slr201.projects.robotsim.service.simulation/  # Microservice
│   └── demo/
│       ├── src/main/java/com/example/demo/
│       │   ├── DemoApplication.java         # Main entry point
│       │   ├── controller/                  # REST controllers
│       │   │   └── SimulationController.java
│       │   ├── service/                     # Business logic
│       │   │   └── SimulationService.java
│       │   └── kafka/                       # Kafka integration
│       │       ├── KafkaConfig.java
│       │       └── KafkaFactoryModelChangeNotifier.java
│       ├── src/main/resources/
│       │   └── application.yml              # Configuration
│       ├── test-endpoints.http              # API test file
│       └── pom.xml                          # Maven dependencies
│
├── docker-compose.yml                       # Kafka setup
└── README.md                                # This file
```

---

## 🎓 Educational Value

This project demonstrates key software engineering concepts:

1. **Design Patterns**:
   - Observer Pattern (model-view updates)
   - Factory Pattern (component creation)
   - Strategy Pattern (path finding algorithms)
   - Singleton Pattern (persistence managers)

2. **Distributed Systems**:
   - Microservices communication
   - Event-driven architecture
   - Message queue integration
   - Service discovery and coordination

3. **Software Architecture**:
   - Separation of concerns
   - Dependency injection
   - RESTful API design
   - Layered architecture (Controller → Service → Repository)

4. **Data Management**:
   - JSON serialization strategies
   - Bidirectional relationship handling
   - Polymorphic type serialization
   - Data consistency in distributed systems

5. **Concurrency**:
   - Multi-threaded component simulation
   - Thread-safe operations
   - Asynchronous message processing
   - GUI event dispatch thread management

---

## 🐛 Troubleshooting

### Common Issues

**Issue**: Kafka container fails to start
```
Solution: Check Docker is running, ports 9092/2181 are free
Commands:
  docker ps
  docker-compose down
  docker-compose up -d
```

**Issue**: GUI windows not appearing
```
Solution: Add JVM argument -Djava.awt.headless=false
Location: Run Configuration → VM Options
```

**Issue**: "Port 8090 already in use"
```
Solution: Stop other applications on port 8090
Commands (PowerShell):
  netstat -ano | findstr :8090
  taskkill /PID <process_id> /F
```

**Issue**: Factory files not found
```
Solution: Check file paths in application.yml
Ensure files are in: fr.tp.inf112.projects.robotsim/*.factory
```

## 📄 License

This project is created for educational purposes as part of an advanced software engineering course.

---

**Course**: Programmation avancée et gestion de projets en Java
**Last Updated**: November 23, 2025
**Institution**: Télécom Paris
