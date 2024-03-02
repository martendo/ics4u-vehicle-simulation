import greenfoot.World;
import java.awt.Graphics2D;

/**
 * The "Vehicle" of the simulation.
 *
 * @author Martin Baldwin
 */
public class Dessert extends SuperActor {
	// Distance to travel along a path per act
	public static final double SPEED = 1.0;

	// The iterator used to travel along this dessert's path
	private PathTraceIterator pathIter;

	/**
	 * Create a new dessert that follows the given path.
	 *
	 * @param path the path for this dessert to travel along
	 */
	public Dessert(SuperPath path) {
		super();
		pathIter = path.getPathTraceIterator();
		double[] coords = new double[6];
		pathIter.currentSegment(coords);
		x = coords[0];
		y = coords[0];
	}

	/**
	 * Update this dessert.
	 */
	public void act() {
		// Move along the path a distance equal to dessert speed
		pathIter.next(SPEED);
		//
		if (pathIter.isDone()) {
			die();
			return;
		}
		// Set this dessert's location to the end point of the iterator's current line segment
		double[] coords = new double[6];
		pathIter.currentSegment(coords);
		x = coords[0];
		y = coords[1];
	}

	/**
	 * Draw this dessert.
	 */
	public void drawUsingGraphics(Graphics2D graphics) {
		graphics.setColor(java.awt.Color.GREEN);
		graphics.fillRect((int) x - 30, (int) y - 30, 60, 60);
	}
}
