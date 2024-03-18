import java.awt.image.BufferedImage;
import greenfoot.util.GraphicsUtilities;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * An object that travels along a path at a constant speed, rotating to reflect
 * its journey along the path.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class PathTraveller extends SuperActor {
	private static final double ANGLE_INTERPOLATION_FACTOR = 0.1;

	private SuperPath path = null;
	// The iterator used to progressively travel along this traveller's path
	private PathTraceIterator pathIter = null;

	// Distance to travel along a path per act
	private double speed;

	// The exact angle of the current segment of the path, which this traveller is constantly interpolating towards
	private double targetAngle = 0.0;

	public PathTraveller(double speed) {
		super();
		this.speed = speed;
	}

	/**
	 * Set the speed of this traveller to the given value.
	 */
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * Return the current speed of this traveller.
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * A callback method that is called when this traveller is added to a SuperPath.
	 *
	 * @param path the path for this traveller to travel along
	 * @param laneNum the index of the lane within the path this traveller was added to
	 */
	public void addedToPath(SuperPath path, int laneNum) {
		this.path = path;
		pathIter = path.getLaneTraceIterator(laneNum);
		// First segment must be PathIterator.SEG_MOVETO
		double[] coords = new double[6];
		pathIter.currentSegment(coords);
		setLocation(coords[0], coords[0]);
		// Start with average angle of path's beginning
		targetAngle = path.getStartAngle();
		setRotation(targetAngle);
		// Reflect angle change in image
		updateImage();
	}

	/**
	 * Return the path that this traveller is currently travelling.
	 */
	public SuperPath getPath() {
		return path;
	}

	/**
	 * Move this traveller along its path, and
	 */
	@Override
	public void act() {
		// Move along the path a distance equal to traveller speed
		pathIter.next(speed);
		if (pathIter.isDone()) {
			endTravel();
			return;
		}
		// Set this traveller's location to the end point of the iterator's current line segment
		double[] coords = new double[6];
		pathIter.currentSegment(coords);
		// Target angle between previous and new position
		targetAngle = Math.atan2(coords[1] - getY(), coords[0] - getX());
		double angle = getRotation();
		// Interpolate angle towards targetAngle, wrapping around when one direction is closer than the current
		if (Math.abs(targetAngle - angle) > Math.abs(targetAngle - (angle - Math.PI * 2.0))) {
			angle -= Math.PI * 2.0;
		} else if (Math.abs(targetAngle - angle) > Math.abs(targetAngle - (angle + Math.PI * 2.0))) {
			angle += Math.PI * 2.0;
		}
		angle += (targetAngle - angle) * ANGLE_INTERPOLATION_FACTOR;
		setRotation(angle);

		// Update position
		setLocation(coords[0], coords[1]);

		// Reflect angle change in image
		updateImage();
	}

	/**
	 * Kill this traveller and remove it from its path.
	 */
	@Override
	public void die() {
		super.die();
		path.removeTraveller(this);
	}

	/**
	 * This method is called when this traveller reaches the end of its path.
	 *
	 * By default, simply kill this traveller.
	 */
	protected void endTravel() {
		die();
	}
}
