import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

/**
 * A visual element shown when zapper traveller zaps a UFO.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Zap extends Effect {
	// Number of acts to keep an explosion on-screen
	public static final int LIFESPAN = 10;

	private static final java.awt.Color COLOR = new java.awt.Color(0, 255, 255);
	private static final int THICKNESS = 20;

	private final BufferedImage image;

	public Zap(double x1, double y1, double x2, double y2) {
		super(LIFESPAN);

		// Initialize image
		image = GraphicsUtilities.createCompatibleTranslucentImage((int) Math.hypot(x2 - x1, y2 - y1), THICKNESS);
		Graphics2D graphics = image.createGraphics();
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);
		graphics.setColor(COLOR);
		graphics.fillRoundRect(0, 0, image.getWidth(), THICKNESS, THICKNESS, THICKNESS);

		setLocation(x2, y2);
		setRotation(Math.atan2(y2 - y1, x2 - x1));
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}
}
