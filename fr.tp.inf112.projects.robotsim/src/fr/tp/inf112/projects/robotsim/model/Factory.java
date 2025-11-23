package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import fr.tp.inf112.projects.canvas.controller.Observable;
import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.Figure;
import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Factory extends Component implements Canvas, Observable {
	private static final Logger LOGGER = Logger.getLogger(Factory.class.getName());
	
	// Empty constructor for Jackson
	public Factory() {
		this(0, 0, null);
		// Ensure notifier is initialized even for Jackson deserialization
		if (this.notifier == null) {
			this.notifier = new LocalFactoryModelChangedNotifier();
		}
	}

	private static final long serialVersionUID = 5156526483612458192L;
	
	private static final ComponentStyle DEFAULT = new ComponentStyle(5.0f);


	@JsonManagedReference
    private final List<Component> components;

	@JsonIgnore
	private transient FactoryModelChangedNotifier notifier;

	@JsonInclude
	private boolean simulationStarted;
	
	public Factory(final int width,
				   final int height,
				   final String name ) {
		super(null, new RectangularShape(0, 0, width, height), name);
		
		components = new ArrayList<>();
		notifier = new LocalFactoryModelChangedNotifier();
		simulationStarted = false;
	}
	
	/**
	 * Gets the notifier used to notify observers when the model changes.
	 * 
	 * @return The notifier instance
	 */
	public FactoryModelChangedNotifier getNotifier() {
		return notifier;
	}
	
	/**
	 * Sets the notifier to be used to notify observers when the model changes.
	 * This allows for different notification mechanisms (local, Kafka, etc.).
	 * 
	 * @param notifier The notifier to use
	 */
	public void setNotifier(final FactoryModelChangedNotifier notifier) {
		this.notifier = notifier;
	}

	@Override
	public boolean addObserver(Observer observer) {
		if (notifier != null) {
			return notifier.addObserver(observer);
		}
		return false;
	}

	@Override
	public boolean removeObserver(Observer observer) {
		if (notifier != null) {
			return notifier.removeObserver(observer);
		}
		return false;
	}
	
	protected void notifyObservers() {
		if (notifier != null) {
			notifier.notifyObservers();
		}
		//  for (final Observer observer : getObservers()) {
		//      observer.modelChanged();
		//  }
	}
	
	public boolean addComponent(final Component component) {
		if (components.add(component)) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	public boolean removeComponent(final Component component) {
		if (components.remove(component)) {
			notifyObservers();
			
			return true;
		}
		
		return false;
	}

	public List<Component> getComponents() {
		return components;
	}
	
	@JsonIgnore
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Collection<Figure> getFigures() {
		return (Collection) components;
	}	@Override
	public String toString() {
		return super.toString() + " components=" + components + "]";
	}
	
	public boolean isSimulationStarted() {
		return simulationStarted;
	}

	public void startSimulation() {
		if (!isSimulationStarted()) {
			this.simulationStarted = true;
			notifyObservers();
			
			// Start parallel execution of components
			behave();
		}
	}

	public void stopSimulation() {
		if (isSimulationStarted()) {
			this.simulationStarted = false;
			
			notifyObservers();
		}
	}

	@Override
	public boolean behave() {
		boolean behaved = true;
		
		for (final Component component : getComponents()) {
			// Create and start a new thread for each component
			final Thread componentThread = new Thread(component);
			componentThread.start();
		}
		
		return behaved;
	}
	
	@JsonIgnore
	@Override
	public Style getStyle() {
		return DEFAULT;
	}
	
	public boolean hasObstacleAt(final PositionedShape shape) {
		for (final Component component : getComponents()) {
			if (component.overlays(shape) && !component.canBeOverlayed(shape)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasMobileComponentAt(final PositionedShape shape,
										final Component movingComponent) {
		for (final Component component : getComponents()) {
			if (component != movingComponent && component.isMobile() && component.overlays(shape)) {
				return true;
			}
		}
		
		return false;
	}
	
	public Component getMobileComponentAt(	final Position position,
											final Component ignoredComponent) {
		if (position == null) {
			return null;
		}
		
		return getMobileComponentAt(new RectangularShape(position.getxCoordinate(), position.getyCoordinate(), 2, 2), ignoredComponent);
	}
	
	public Component getMobileComponentAt(	final PositionedShape shape,
											final Component ignoredComponent) {
		if (shape == null) {
			return null;
		}
		
		for (final Component component : getComponents()) {
			if (component != ignoredComponent && component.isMobile() && component.overlays(shape)) {
				return component;
			}
		}
		
		return null;
	}
	
	/**
	 * Synchronized method to move a component safely in a multi-threaded environment.
	 * This method checks if the target position is free before moving the component.
	 * 
	 * @param motion The motion object containing the movement details
	 * @param componentToMove The component that wants to move
	 * @return The displacement achieved by the movement (0 if no movement occurred)
	 */
	public synchronized int moveComponent(final Motion motion, final Component componentToMove) {
		if (motion == null || componentToMove == null) {
			return 0;
		}
		
		final Position targetPosition = motion.getTargetPosition();
		final PositionedShape targetShape = new RectangularShape(targetPosition.getxCoordinate(),
																 targetPosition.getyCoordinate(),
																 componentToMove.getWidth(),
																 componentToMove.getHeight());
		
		// Check if the target position is free of mobile components
		if (hasMobileComponentAt(targetShape, componentToMove)) {
			// Position is occupied, cannot move
			LOGGER.fine(componentToMove.getName() + " blocked by another robot at " + targetPosition);
			return 0;
		}
		
		// Check if there are obstacles at the target position
		if (hasObstacleAt(targetShape)) {
			// Position has obstacles, cannot move
			return 0;
		}
		
		// Position is free, perform the movement
		LOGGER.fine(componentToMove.getName() + " moving to " + targetPosition);
		return motion.moveToTarget();
	}
}
