package fr.tp.inf112.projects.robotsim.model;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

/**
 * Simple test to verify that the refactored Factory model with notifier
 * works correctly. This test creates a factory, adds an observer, and
 * verifies that the observer is notified when components are added.
 */
public class NotifierTest {
    
    private static boolean observerWasNotified = false;
    
    public static void main(String[] args) {
        System.out.println("=== Testing Factory Model with Notifier ===\n");
        
        // Create a factory - should automatically get LocalFactoryModelChangedNotifier
        Factory factory = new Factory(200, 200, "Test Factory");
        System.out.println("✓ Created factory: " + factory.getName());
        System.out.println("✓ Notifier type: " + factory.getNotifier().getClass().getSimpleName());
        
        // Create a test observer
        Observer testObserver = new Observer() {
            @Override
            public void modelChanged() {
                observerWasNotified = true;
                System.out.println("✓ Observer was notified of model change!");
            }
        };
        
        // Add the observer
        boolean added = factory.addObserver(testObserver);
        System.out.println("✓ Observer added: " + added);
        
        // Add a component - should trigger notification
        System.out.println("\nAdding a room to the factory...");
        Room room = new Room(factory, new RectangularShape(20, 20, 50, 50), "Test Room");
        
        // Check if observer was notified
        if (observerWasNotified) {
            System.out.println("✓ SUCCESS: Observer was notified when component was added!");
        } else {
            System.out.println("✗ FAILURE: Observer was NOT notified!");
            System.exit(1);
        }
        
        // Reset flag
        observerWasNotified = false;
        
        // Test removing observer
        System.out.println("\nRemoving observer...");
        boolean removed = factory.removeObserver(testObserver);
        System.out.println("✓ Observer removed: " + removed);
        
        // Add another component - should NOT trigger notification
        System.out.println("\nAdding another room after observer removal...");
        Room room2 = new Room(factory, new RectangularShape(80, 80, 50, 50), "Test Room 2");
        
        if (!observerWasNotified) {
            System.out.println("✓ SUCCESS: Observer was NOT notified after removal!");
        } else {
            System.out.println("✗ FAILURE: Observer was notified even after removal!");
            System.exit(1);
        }
        
        // Test setting a custom notifier
        System.out.println("\n=== Testing Custom Notifier ===");
        FactoryModelChangedNotifier customNotifier = new LocalFactoryModelChangedNotifier();
        factory.setNotifier(customNotifier);
        System.out.println("✓ Custom notifier set");
        
        // Add observer to custom notifier
        factory.addObserver(testObserver);
        observerWasNotified = false;
        
        // Add component - should trigger notification via custom notifier
        System.out.println("\nAdding component with custom notifier...");
        Room room3 = new Room(factory, new RectangularShape(140, 140, 50, 50), "Test Room 3");
        
        if (observerWasNotified) {
            System.out.println("✓ SUCCESS: Custom notifier works correctly!");
        } else {
            System.out.println("✗ FAILURE: Custom notifier did not work!");
            System.exit(1);
        }
        
        System.out.println("\n=== All Tests Passed! ===");
        System.out.println("The Factory model has been successfully refactored to use");
        System.out.println("a separate notifier class, following the Single Responsibility Principle.");
    }
}
