package fr.tp.inf112.projects.robotsim.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.tp.inf112.projects.canvas.model.Style;
import fr.tp.inf112.projects.canvas.model.impl.RGBColor;
import fr.tp.inf112.projects.robotsim.model.motion.Motion;
import fr.tp.inf112.projects.robotsim.model.path.FactoryPathFinder;
import fr.tp.inf112.projects.robotsim.model.shapes.CircularShape;
import fr.tp.inf112.projects.robotsim.model.shapes.PositionedShape;
import fr.tp.inf112.projects.robotsim.model.shapes.RectangularShape;

public class Robot extends Component {
	private static final Logger LOGGER = Logger.getLogger(Robot.class.getName());
	
	// Empty constructor for Jackson
	public Robot() {
		this(null, null, null, null, null);
	}
	
	private static final long serialVersionUID = -1218857231970296747L;

	private static final Style STYLE = new ComponentStyle(RGBColor.GREEN, RGBColor.BLACK, 3.0f, null);

	private static final Style BLOCKED_STYLE = new ComponentStyle(RGBColor.RED, RGBColor.BLACK, 3.0f, new float[]{4.0f});

	private final Battery battery;
	
	private int speed;
	
	private List<Component> targetComponents;
	
	@JsonIgnore
	private transient Iterator<Component> targetComponentsIterator;
	
	private Component currTargetComponent;
	
	@JsonIgnore
	private transient Iterator<Position> currentPathPositionsIter;
	
	@JsonIgnore
	private transient boolean blocked;
	
	private Position memorizedTargetPosition;
	
	@JsonIgnore
	private transient int waitCounter;
	
	private FactoryPathFinder pathFinder;

	public Robot(final Factory factory,
				 final FactoryPathFinder pathFinder,
				 final CircularShape shape,
				 final Battery battery,
				 final String name ) {
		super(factory, shape, name);
		
		this.pathFinder = pathFinder;
		
		this.battery = battery;
		
		targetComponents = new ArrayList<>();
		currTargetComponent = null;
		currentPathPositionsIter = null;
		speed = 5;
		blocked = false;
		memorizedTargetPosition = null;
		waitCounter = 0;
	}

	@Override
	public String toString() {
		return super.toString() + " battery=" + battery + "]";
	}

	protected int getSpeed() {
		return speed;
	}

	protected void setSpeed(final int speed) {
		this.speed = speed;
	}
	
	public Position getMemorizedTargetPosition() {
		return memorizedTargetPosition;
	}
	
	private List<Component> getTargetComponents() {
		if (targetComponents == null) {
			targetComponents = new ArrayList<>();
		}
		
		return targetComponents;
	}
	
	public boolean addTargetComponent(final Component targetComponent) {
		return getTargetComponents().add(targetComponent);
	}
	
	public boolean removeTargetComponent(final Component targetComponent) {
		return getTargetComponents().remove(targetComponent);
	}
	
	@Override
	public boolean isMobile() {
		return true;
	}

	@Override
	public boolean behave() {
		if (getTargetComponents().isEmpty()) {
			return false;
		}
		
		if (currTargetComponent == null || hasReachedCurrentTarget()) {
			if (currTargetComponent != null) {
				LOGGER.info(getName() + " REACHED target: " + currTargetComponent.getName() + 
								   " at " + getPosition());
			}
			currTargetComponent = nextTargetComponentToVisit();
			LOGGER.info(getName() + " NEW target: " + 
							   (currTargetComponent != null ? currTargetComponent.getName() : "null"));
			
			computePathToCurrentTargetComponent();
		}

		return moveToNextPathPosition() != 0;
	}


	private Component nextTargetComponentToVisit() {
		if (targetComponentsIterator == null || !targetComponentsIterator.hasNext()) {
			targetComponentsIterator = getTargetComponents().iterator();
		}
		
		return targetComponentsIterator.hasNext() ? targetComponentsIterator.next() : null;
	}

	private int moveToNextPathPosition() {
		final Motion motion = computeMotion();
		// Use synchronized factory method to move safely
		int displacement = motion == null ? 0 : getFactory().moveComponent(motion, this);
		
		if (displacement != 0) {
			// Successfully moved, reset wait counter
			waitCounter = 0;
			notifyObservers();
		} 
		else if (isLivelyLocked()) {
			// Classic livelock detected - step aside immediately
			LOGGER.warning(getName() + " LIVELOCK DETECTED at " + getPosition() + 
							   " - trying to reach " + memorizedTargetPosition);
			final Position freeNeighbouringPosition = findFreeNeighbouringPosition();
			if (freeNeighbouringPosition != null) {
				LOGGER.info(getName() + " Stepping aside to " + freeNeighbouringPosition);
				// Move the robot to the free neighboring position
				setxCoordinate(freeNeighbouringPosition.getxCoordinate());
				setyCoordinate(freeNeighbouringPosition.getyCoordinate());
				// Clear the memorized target position
				memorizedTargetPosition = null;
				waitCounter = 0;
				// Recompute the path from this new position
				computePathToCurrentTargetComponent();
				notifyObservers();
			} else {
				LOGGER.warning(getName() + " CANNOT find free neighbor position!");
			}
		}
		else {
			// Not a livelock, but blocked - increment wait counter
			waitCounter++;
			
			if (waitCounter == 1) {
				LOGGER.fine(getName() + " BLOCKED at " + getPosition() + 
								   " - target: " + (currTargetComponent != null ? currTargetComponent.getName() : "null") +
								   " - memorized: " + memorizedTargetPosition);
				
				// If memorized is null, it means we have no path - try recomputing immediately
				if (memorizedTargetPosition == null) {
					LOGGER.fine(getName() + " No memorized position - recomputing path");
					computePathToCurrentTargetComponent();
					
					// If still no path after recomputing, target is unreachable
					if (!currentPathPositionsIter.hasNext()) {
						LOGGER.severe(getName() + " ERROR: Target " + currTargetComponent.getName() + 
										   " is UNREACHABLE from " + getPosition() + " - skipping to next target");
						currTargetComponent = nextTargetComponentToVisit();
						computePathToCurrentTargetComponent();
						waitCounter = 0;
						return displacement;
					}
				}
			}
			
			// After waiting for a while, try to step aside to let other robot pass
			if (waitCounter > 10) {
				LOGGER.info(getName() + " Waited " + waitCounter + " iterations, trying to step aside");
				final Position freeNeighbouringPosition = findFreeNeighbouringPosition();
				if (freeNeighbouringPosition != null) {
					LOGGER.info(getName() + " Stepping aside to " + freeNeighbouringPosition);
					// Move the robot to the free neighboring position
					setxCoordinate(freeNeighbouringPosition.getxCoordinate());
					setyCoordinate(freeNeighbouringPosition.getyCoordinate());
					// Clear the memorized target position
					memorizedTargetPosition = null;
					waitCounter = 0;
					// Recompute the path from this new position
					computePathToCurrentTargetComponent();
					notifyObservers();
				} else {
					LOGGER.fine(getName() + " Cannot find free neighbor, resetting counter");
					// Can't step aside, reset counter to try again later
					waitCounter = 5;
				}
			} else if (waitCounter % 5 == 0) {
				LOGGER.fine(getName() + " Still waiting... (" + waitCounter + " iterations)");
			}
		}
		
		return displacement;
	}
	
	private void computePathToCurrentTargetComponent() {
		final List<Position> currentPathPositions = pathFinder.findPath(this, currTargetComponent);
		currentPathPositionsIter = currentPathPositions.iterator();
		
		LOGGER.fine(getName() + " Computed path to " + 
						   (currTargetComponent != null ? currTargetComponent.getName() : "null") + 
						   " - path has " + (currentPathPositions != null ? currentPathPositions.size() : 0) + " positions");
	}
	
	private Motion computeMotion() {
		if (!currentPathPositionsIter.hasNext()) {

			// There is no free path to the target
			blocked = true;
			
			return null;
		}
		
		
		final Position targetPosition = getTargetPosition();
		final PositionedShape shape = new RectangularShape(targetPosition.getxCoordinate(),
														   targetPosition.getyCoordinate(),
				   										   2,
				   										   2);
		
		// If there is another robot, memorize the target position for the next run
		if (getFactory().hasMobileComponentAt(shape, this)) {
			this.memorizedTargetPosition = targetPosition;
			blocked = true;
			
			return null;
		}

		// Reset the memorized position and blocked state
		this.memorizedTargetPosition = null;
		blocked = false;
			
		return new Motion(getPosition(), targetPosition);
	}
	
	private Position getTargetPosition() {
		// If a target position was memorized, it means that the robot was blocked during the last iteration 
		// so it waited for another robot to pass. So try to move to this memorized position otherwise move to  
		// the next position from the path
		return this.memorizedTargetPosition == null ? currentPathPositionsIter.next() : this.memorizedTargetPosition;
	}


	@JsonIgnore
	public boolean isLivelyLocked() {
		final Position memorizedTargetPosition = getMemorizedTargetPosition();
		if (memorizedTargetPosition == null) {
	        return false;
	    }

		final Component otherRobot = getFactory().getMobileComponentAt(memorizedTargetPosition, this);
		if (otherRobot != null && otherRobot instanceof Robot) {
			final Position otherMemorized = ((Robot)otherRobot).getMemorizedTargetPosition();
			boolean isLocked = getPosition().equals(otherMemorized);
			if (isLocked) {
				LOGGER.fine(getName() + " Livelock check: YES - " +
								   "I'm at " + getPosition() + " wanting " + memorizedTargetPosition +
								   ", " + otherRobot.getName() + " is at " + otherRobot.getPosition() + 
								   " wanting " + otherMemorized);
			}
			return isLocked;
		}
		return false;
	}

	private boolean hasReachedCurrentTarget() {
		return getPositionedShape().overlays(currTargetComponent.getPositionedShape());
	}
	
	private Position findFreeNeighbouringPosition() {
		final Position currentPosition = getPosition();
		final int x = currentPosition.getxCoordinate();
		final int y = currentPosition.getyCoordinate();
		
		// Check 8 neighboring positions (up, down, left, right, and diagonals)
		final int[] deltaX = {-1, -1, -1, 0, 0, 1, 1, 1};
		final int[] deltaY = {-1, 0, 1, -1, 1, -1, 0, 1};
		
		for (int i = 0; i < deltaX.length; i++) {
			final Position neighborPosition = new Position(x + deltaX[i], y + deltaY[i]);
			
			// Skip positions that would be out of bounds (negative coordinates)
			if (neighborPosition.getxCoordinate() < 0 || neighborPosition.getyCoordinate() < 0) {
				continue;
			}
			
			final PositionedShape neighborShape = new RectangularShape(neighborPosition.getxCoordinate(),
																	   neighborPosition.getyCoordinate(),
																	   getWidth(),
																	   getHeight());
			
			// Check if this neighboring position is free of mobile components and obstacles
			if (!getFactory().hasMobileComponentAt(neighborShape, this) && 
				!getFactory().hasObstacleAt(neighborShape)) {
				return neighborPosition;
			}
		}
		
		// No free neighboring position found
		return null;
	}
	
	/**
	 * Finds a waiting position away from the current target for yielding to another robot.
	 * Tries to move in the opposite direction of the target to get out of the way.
	 */
	private Position findWaitingPosition() {
		final Position currentPosition = getPosition();
		final Position targetPosition = currTargetComponent.getPosition();
		
		// Calculate direction away from target
		final int dx = currentPosition.getxCoordinate() - targetPosition.getxCoordinate();
		final int dy = currentPosition.getyCoordinate() - targetPosition.getyCoordinate();
		
		// Try positions moving away from target (3-5 steps back)
		for (int distance = 3; distance <= 5; distance++) {
			int moveX = 0;
			int moveY = 0;
			
			// Normalize direction and move away
			if (dx != 0) {
				moveX = (dx > 0 ? distance : -distance);
			}
			if (dy != 0) {
				moveY = (dy > 0 ? distance : -distance);
			}
			
			// If no clear direction, try diagonal moves
			if (moveX == 0 && moveY == 0) {
				moveX = distance;
				moveY = distance;
			}
			
			final Position waitPosition = new Position(
				currentPosition.getxCoordinate() + moveX,
				currentPosition.getyCoordinate() + moveY
			);
			
			// Skip out of bounds
			if (waitPosition.getxCoordinate() < 0 || waitPosition.getyCoordinate() < 0) {
				continue;
			}
			
			final PositionedShape waitShape = new RectangularShape(
				waitPosition.getxCoordinate(),
				waitPosition.getyCoordinate(),
				getWidth(),
				getHeight()
			);
			
			// Check if position is free
			if (!getFactory().hasMobileComponentAt(waitShape, this) && 
				!getFactory().hasObstacleAt(waitShape)) {
				return waitPosition;
			}
		}
		
		// Fallback: try any neighboring position
		return findFreeNeighbouringPosition();
	}
	
	@Override
	public boolean canBeOverlayed(final PositionedShape shape) {
		return true;
	}
	
	@JsonIgnore
	@Override
	public Style getStyle() {
		return blocked ? BLOCKED_STYLE : STYLE;
	}
}
