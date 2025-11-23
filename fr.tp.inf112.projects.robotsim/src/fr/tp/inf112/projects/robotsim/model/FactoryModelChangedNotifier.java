package fr.tp.inf112.projects.robotsim.model;

import fr.tp.inf112.projects.canvas.controller.Observer;

/**
 * Interface for notifying observers when the factory model changes.
 * This interface separates the responsibility of notifying observers from
 * the factory model itself, following the Single Responsibility Principle (SRP).
 * 
 * Different implementations can use different notification mechanisms:
 * - Local notification: directly calling observer methods
 * - Remote notification: using Kafka or other messaging systems
 */
public interface FactoryModelChangedNotifier {
    
    /**
     * Notifies all registered observers that the factory model has changed.
     */
    void notifyObservers();
    
    /**
     * Adds an observer to be notified when the factory model changes.
     * 
     * @param observer The observer to add
     * @return true if the observer was successfully added, false otherwise
     */
    boolean addObserver(Observer observer);
    
    /**
     * Removes an observer from the notification list.
     * 
     * @param observer The observer to remove
     * @return true if the observer was successfully removed, false otherwise
     */
    boolean removeObserver(Observer observer);
}
