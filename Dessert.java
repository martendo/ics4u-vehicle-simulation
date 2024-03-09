import greenfoot.World;
import java.awt.Graphics2D;

/**
 * The "Vehicle" of the simulation.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Dessert extends PathTraveller {
	/**
	 * Create a new dessert.
	 */
	public Dessert(SuperPath path, int laneNum) {
		super(path, laneNum);
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
		graphics.fillRect((int) x - 10, (int) y - 10, 20, 20);
	}
}
