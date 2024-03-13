/**
 * An object that travels along a path at a constant speed, rotating to reflect
 * its journey along the path.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class PathTraveller extends SuperActor {
	// Distance to travel along a path per act
	public static final double SPEED = 1.0;

	private SuperPath path;
	// The iterator used to progressively travel along this traveller's path
	private PathTraceIterator pathIter;

	// The exact angle of the current segment of the path
	private double targetAngle;
	// The visually pleasing interpolated angle of this traveller, constantly moving towards targetAngle
	protected double angle;

	/**
	 * Create a new traveller actor.
	 */
	public PathTraveller() {
		super();
		path = null;
		pathIter = null;
		targetAngle = 0.0;
		angle = 0.0;
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
		x = coords[0];
		y = coords[0];
		// Start with average angle of path's beginning
		targetAngle = path.getStartAngle();
		angle = targetAngle;
	}

	/**
	 * Move this traveller along its path, and
	 */
	public void act() {
		// Move along the path a distance equal to traveller speed
		pathIter.next(SPEED);
		if (pathIter.isDone()) {
			endTravel();
			return;
		}
		// Set this traveller's location to the end point of the iterator's current line segment
		double[] coords = new double[6];
		pathIter.currentSegment(coords);
		// Target angle between previous and new position
		targetAngle = Math.atan2(coords[1] - y, coords[0] - x);
		// Interpolate angle towards targetAngle, wrapping around when one direction is closer than the current
		if (Math.abs(targetAngle - angle) > Math.abs(targetAngle - (angle - Math.PI * 2.0))) {
			angle -= Math.PI * 2.0;
		} else if (Math.abs(targetAngle - angle) > Math.abs(targetAngle - (angle + Math.PI * 2.0))) {
			angle += Math.PI * 2.0;
		}
		angle += (targetAngle - angle) * 0.05;

		// Update position
		x = coords[0];
		y = coords[1];
	}

	/**
	 * Kill this traveller and remove it from its path.
	 */
	public void die() {
		super.die();
		path.removeTraveller(this);
	}

	/**
	 * This method is called when this traveller reaches the end of its path.
	 */
	protected abstract void endTravel();
}
