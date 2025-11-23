package fr.tp.inf112.projects.robotsim.model.shapes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.tp.inf112.projects.canvas.model.RectangleShape;

public class RectangularShape extends PositionedShape implements RectangleShape {
	
	private static final long serialVersionUID = -6113167952556242089L;

	@JsonProperty("width")
	private final int width;

	@JsonProperty("heigth")
	private final int heigth;

	// Empty constructor for Jackson
	public RectangularShape() {
		this(0, 0, 0, 0);
	}

	@JsonCreator
	public RectangularShape(@JsonProperty("xCoordinate") final int xCoordinate,
							@JsonProperty("yCoordinate") final int yCoordinate,
							@JsonProperty("width") final int width,
							@JsonProperty("heigth") final int heigth) {
		super(xCoordinate, yCoordinate);
	
		this.width = width;
		this.heigth = heigth;
	}

	@JsonIgnore
	@Override
	public int getWidth() {
		return width;
	}

	@JsonIgnore
	@Override
	public int getHeight() {
		return heigth;
	}

	@Override
	public String toString() {
		return super.toString() + " [width=" + width + ", heigth=" + heigth + "]";
	}
}
