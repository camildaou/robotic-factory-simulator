package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.List;

import fr.tp.inf112.projects.canvas.controller.Observer;

/**
 * Local implementation of FactoryModelChangedNotifier that directly notifies
 * observers in the same JVM process. This is used when the factory model and
 * the user interface are running on the same computer.
 * 
 * This class maintains the list of observers and calls their modelChanged()
 * method when notifyObservers() is invoked.
 */
public class LocalFactoryModelChangedNotifier implements FactoryModelChangedNotifier {
    
    /**
     * List of observers to be notified when the model changes.
     */
    private List<Observer> observers;
    
    /**
     * Creates a new LocalFactoryModelChangedNotifier with an empty observer list.
     */
    public LocalFactoryModelChangedNotifier() {
        this.observers = null;
    }
    
    /**
     * Gets the list of observers, initializing it if necessary.
     * 
     * @return The list of observers
     */
    protected List<Observer> getObservers() {
        if (observers == null) {
            observers = new ArrayList<>();
        }
        
        return observers;
    }
    
    /**
     * {@inheritDoc}
     * 
     * Notifies all registered observers by calling their modelChanged() method.
     */
    @Override
    public void notifyObservers() {
        for (final Observer observer : getObservers()) {
            observer.modelChanged();
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * Adds an observer to the notification list.
     */
    @Override
    public boolean addObserver(final Observer observer) {
        if (observer == null) {
            return false;
        }
        
        return getObservers().add(observer);
    }
    
    /**
     * {@inheritDoc}
     * 
     * Removes an observer from the notification list.
     */
    @Override
    public boolean removeObserver(final Observer observer) {
        if (observer == null) {
            return false;
        }
        
        return getObservers().remove(observer);
    }
}
