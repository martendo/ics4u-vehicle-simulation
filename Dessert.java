import greenfoot.World;
import java.awt.Graphics2D;

/**
 * The "Vehicle" of the simulation.
 *
 * @author Martin Baldwin
 */
public class Dessert extends PathTraveller {
	/**
	 * Create a new dessert that follows the given path.
	 *
	 * @param path the path for this dessert to travel along
	 */
	public Dessert(SuperPath path) {
		super(path);
	}

	/**
	 * Kill this dessert when it reached the end of its path.
	 */
	protected void endTravel() {
		die();
	}

	/**
	 * Draw this dessert.
	 */
	public void drawUsingGraphics(Graphics2D graphics) {
		graphics.setColor(java.awt.Color.GREEN);
		graphics.fillRect((int) x - 30, (int) y - 30, 60, 60);
	}
}
