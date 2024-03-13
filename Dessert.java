import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The "Vehicle" of the simulation.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Dessert extends PathTraveller {
	private BufferedImage image;
	private Graphics2D graphics;
	private Rectangle2D.Double shape;

	public Dessert() {
		super();
		image = GraphicsUtilities.createCompatibleTranslucentImage(64, 64);
		graphics = image.createGraphics();
		shape = new Rectangle2D.Double(-30, -20 / 2, 30, 20);
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));
		graphics.setColor(java.awt.Color.GREEN);
	}

	public void addedToPath(SuperPath path, int laneNum) {
		super.addedToPath(path, laneNum);
		// Reflect angle change in image
		updateImage();
	}

	public void act() {
		super.act();
		// Reflect angle change in image
		updateImage();
	}

	private void updateImage() {
		graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
		AffineTransform saveAT = graphics.getTransform();
		// Rotate about and draw the image from the center of this object
		graphics.translate(image.getWidth() / 2, image.getHeight() / 2);
		graphics.rotate(angle);
		graphics.fill(shape);
		graphics.setTransform(saveAT);
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
