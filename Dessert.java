import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

/**
 * The "Vehicle" of the simulation.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Dessert extends PathTraveller {
	private BufferedImage image;

	public Dessert() {
		super();
		image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(java.awt.Color.GREEN);
		graphics.fillRect(0, 0, 20, 20);
		graphics.dispose();
	}

	/**
	 * Kill this dessert when it reaches the end of its path.
	 */
	protected void endTravel() {
		die();
	}

	public BufferedImage getImage() {
		return image;
	}
}
