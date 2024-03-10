/**
 * An object that travels along a path at a constant speed.
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

	/**
	 * Create a new traveller actor.
	 */
	public PathTraveller() {
		super();
		path = null;
		pathIter = null;
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
