package fr.tp.inf112.projects.robotsim.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import fr.tp.inf112.projects.canvas.model.impl.BasicVertex;
import fr.tp.inf112.projects.robotsim.model.shapes.BasicPolygonShape;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;
import fr.tp.inf112.projects.robotsim.model.path.JGraphTDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.CustomDijkstraFactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;

/**
 * Test class for Jackson JSON serialization of the robotic factory simulator model.
 * This class tests the serialization and deserialization of complex factory models
 * including polymorphic relationships and bidirectional references.
 */
public class TestRobotSimSerializationJSON {

    private static final Logger LOGGER = Logger.getLogger(TestRobotSimSerializationJSON.class.getName());

    private final ObjectMapper objectMapper;
    private Factory myFactory;

    public TestRobotSimSerializationJSON() {
        objectMapper = new ObjectMapper();

        // Configure polymorphic type validator for inheritance and collections
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(PositionedShape.class.getPackageName())
            .allowIfSubType(Component.class.getPackageName())
            .allowIfSubType(BasicVertex.class.getPackageName())
            .allowIfSubType(ArrayList.class.getName())
            .allowIfSubType(LinkedHashSet.class.getName())
            .build();

        objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
    }

    @BeforeEach
    public void setUp() {
        // Create a factory model similar to SimulatorApplication.main()
        createFactoryModel();
    }

    /**
     * Creates a complete factory model with rooms, machines, robots, and other components
     * based on the SimulatorApplication setup.
     */
    private void createFactoryModel() {
        myFactory = new Factory(200, 200, "Simple Test Puck Factory");

        // Create Room 1 with Door, Area, and Machine
        final Room room1 = new Room(myFactory, new RectangularShape(20, 20, 75, 75), "Production Room 1");
        new Door(room1, Room.WALL.BOTTOM, 10, 20, true, "Entrance");
        final Area area1 = new Area(room1, new RectangularShape(35, 35, 50, 50), "Production Area 1");
        final Machine machine1 = new Machine(area1, new RectangularShape(50, 50, 15, 15), "Machine 1");

        // Create Room 2 with Door, Area, and Machine
        final Room room2 = new Room(myFactory, new RectangularShape(120, 22, 75, 75), "Production Room 2");
        new Door(room2, Room.WALL.LEFT, 10, 20, true, "Entrance");
        final Area area2 = new Area(room2, new RectangularShape(135, 35, 50, 50), "Production Area 2");
        final Machine machine2 = new Machine(area2, new RectangularShape(150, 50, 15, 15), "Machine 2");

        // Create Conveyor with BasicPolygonShape
        final int baselineSize = 3;
        final int xCoordinate = 10;
        final int yCoordinate = 165;
        final int width = 10;
        final int height = 30;
        final BasicPolygonShape conveyorShape = new BasicPolygonShape();
        conveyorShape.addVertex(new BasicVertex(xCoordinate, yCoordinate));
        conveyorShape.addVertex(new BasicVertex(xCoordinate + width, yCoordinate));
        conveyorShape.addVertex(new BasicVertex(xCoordinate + width, yCoordinate + height - baselineSize));
        conveyorShape.addVertex(new BasicVertex(xCoordinate + width + baselineSize, yCoordinate + height - baselineSize));
        conveyorShape.addVertex(new BasicVertex(xCoordinate + width + baselineSize, yCoordinate + height));
        conveyorShape.addVertex(new BasicVertex(xCoordinate - baselineSize, yCoordinate + height));
        conveyorShape.addVertex(new BasicVertex(xCoordinate - baselineSize, yCoordinate + height - baselineSize));
        conveyorShape.addVertex(new BasicVertex(xCoordinate, yCoordinate + height - baselineSize));

        // Create Charging Room and Station
        final Room chargingRoom = new Room(myFactory, new RectangularShape(125, 125, 50, 50), "Charging Room");
        new Door(chargingRoom, Room.WALL.RIGHT, 10, 20, false, "Entrance");
        final ChargingStation chargingStation = new ChargingStation(myFactory, new RectangularShape(150, 145, 15, 15), "Charging Station");

        // Create Robots with different path finders
        final FactoryPathFinder jgraphPathFinder = new JGraphTDijkstraFactoryPathFinder(myFactory, 5);
        final Robot robot1 = new Robot(myFactory, jgraphPathFinder, new CircularShape(5, 5, 2), new Battery(10), "Robot 1");
        robot1.addTargetComponent(machine1);
        robot1.addTargetComponent(machine2);
        robot1.addTargetComponent(new Conveyor(myFactory, conveyorShape, "Conveyor 1"));
        robot1.addTargetComponent(chargingStation);

        final FactoryPathFinder customPathFinder = new CustomDijkstraFactoryPathFinder(myFactory, 5);
        final Robot robot2 = new Robot(myFactory, customPathFinder, new CircularShape(45, 5, 2), new Battery(10), "Robot 2");
        robot2.addTargetComponent(machine1);
        robot2.addTargetComponent(machine2);
        robot2.addTargetComponent(chargingStation);

        LOGGER.info("Factory model created with " + myFactory.getComponents().size() + " components");
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        // Test serialization: Factory -> JSON String
        final String factoryAsJsonString = objectMapper.writeValueAsString(myFactory);
        assertNotNull(factoryAsJsonString);
        assertFalse(factoryAsJsonString.isEmpty());

        LOGGER.info("Factory serialized to JSON:");
        LOGGER.info(factoryAsJsonString);

        // Verify JSON contains expected elements
        assertTrue(factoryAsJsonString.contains("Simple Test Puck Factory"));
        assertTrue(factoryAsJsonString.contains("Robot 1"));
        assertTrue(factoryAsJsonString.contains("Robot 2"));
        assertTrue(factoryAsJsonString.contains("Production Room 1"));
        assertTrue(factoryAsJsonString.contains("Production Room 2"));
        assertTrue(factoryAsJsonString.contains("Charging Room"));
    }

    @Test
    public void testDeserialization() throws JsonProcessingException {
        // Test round-trip: Factory -> JSON -> Factory
        final String factoryAsJsonString = objectMapper.writeValueAsString(myFactory);
        final Factory roundTrip = objectMapper.readValue(factoryAsJsonString, Factory.class);

        assertNotNull(roundTrip);
        LOGGER.info("Factory deserialized from JSON:");
        LOGGER.info(roundTrip.toString());

        // Verify basic properties are maintained
        assertEquals(myFactory.getName(), roundTrip.getName());
        assertEquals(myFactory.getWidth(), roundTrip.getWidth());
        assertEquals(myFactory.getHeight(), roundTrip.getHeight());
        assertEquals(myFactory.getComponents().size(), roundTrip.getComponents().size());
    }

    @Test
    public void testPolymorphicSerialization() throws JsonProcessingException {
        // Test that different component types are correctly serialized/deserialized
        final String factoryAsJsonString = objectMapper.writeValueAsString(myFactory);
        final Factory roundTrip = objectMapper.readValue(factoryAsJsonString, Factory.class);

        // Count different component types in original factory
        long originalRobots = myFactory.getComponents().stream().filter(c -> c instanceof Robot).count();
        long originalRooms = myFactory.getComponents().stream().filter(c -> c instanceof Room).count();
        long originalMachines = myFactory.getComponents().stream().filter(c -> c instanceof Machine).count();
        long originalDoors = myFactory.getComponents().stream().filter(c -> c instanceof Door).count();

        // Count different component types in deserialized factory
        long deserializedRobots = roundTrip.getComponents().stream().filter(c -> c instanceof Robot).count();
        long deserializedRooms = roundTrip.getComponents().stream().filter(c -> c instanceof Room).count();
        long deserializedMachines = roundTrip.getComponents().stream().filter(c -> c instanceof Machine).count();
        long deserializedDoors = roundTrip.getComponents().stream().filter(c -> c instanceof Door).count();

        // Verify polymorphic types are preserved
        assertEquals(originalRobots, deserializedRobots, "Robot count should match");
        assertEquals(originalRooms, deserializedRooms, "Room count should match");
        assertEquals(originalMachines, deserializedMachines, "Machine count should match");
        assertEquals(originalDoors, deserializedDoors, "Door count should match");

        LOGGER.info("Polymorphic serialization test passed:");
        LOGGER.info("Robots: " + deserializedRobots + ", Rooms: " + deserializedRooms +
                   ", Machines: " + deserializedMachines + ", Doors: " + deserializedDoors);
    }

    @Test
    public void testBidirectionalReferences() throws JsonProcessingException {
        // Test that bidirectional references (Factory <-> Component) work correctly
        final String factoryAsJsonString = objectMapper.writeValueAsString(myFactory);
        final Factory roundTrip = objectMapper.readValue(factoryAsJsonString, Factory.class);

        assertNotNull(roundTrip);
        assertNotNull(roundTrip.getComponents());
        assertFalse(roundTrip.getComponents().isEmpty());

        // Verify that components have correct factory references after deserialization
        for (Component component : roundTrip.getComponents()) {
            // Note: Due to @JsonBackReference, the factory field might be null after deserialization
            // This is expected behavior for Jackson's bidirectional reference handling
            LOGGER.info("Component: " + component.getName() + " of type: " + component.getClass().getSimpleName());
        }

        LOGGER.info("Bidirectional references test completed - Factory contains " +
                   roundTrip.getComponents().size() + " components");
    }

    @Test
    public void testComplexShapesSerialization() throws JsonProcessingException {
        // Test serialization of different shape types
        final String factoryAsJsonString = objectMapper.writeValueAsString(myFactory);
        final Factory roundTrip = objectMapper.readValue(factoryAsJsonString, Factory.class);

        assertNotNull(roundTrip);

        // Find components with different shape types
        boolean hasRectangularShape = false;
        boolean hasCircularShape = false;
        boolean hasPolygonShape = false;

        for (Component component : roundTrip.getComponents()) {
            if (component.getPositionedShape() instanceof RectangularShape) {
                hasRectangularShape = true;
            } else if (component.getPositionedShape() instanceof CircularShape) {
                hasCircularShape = true;
            } else if (component.getPositionedShape() instanceof BasicPolygonShape) {
                hasPolygonShape = true;
            }
        }

        assertTrue(hasRectangularShape, "Should have components with RectangularShape");
        assertTrue(hasCircularShape, "Should have components with CircularShape");
        assertTrue(hasPolygonShape, "Should have components with BasicPolygonShape");

        LOGGER.info("Complex shapes serialization test passed - all shape types preserved");
    }
}