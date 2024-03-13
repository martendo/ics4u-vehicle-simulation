import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The visual object representing the beginnings and ends of paths. Path
 * travellers are drawn under these objects.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Machine extends SuperActor {
	// The fixed width (or depth) of all machines
	public static final int WIDTH = 64;

	// The height of this machine, dependent on the width of its path
	private int height;
	// The path on which this machine appears
	private SuperPath path;

	private BufferedImage image;
	private Graphics2D graphics;
	private Rectangle2D.Double shape;

	public Machine(SuperPath path) {
		super();
		this.path = path;
		height = path.getWidth() + 50;

		// Initialize image
		int imageSize = Math.max(height, WIDTH) * 2;
		image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB_PRE);
		graphics = image.createGraphics();
		shape = new Rectangle2D.Double(-WIDTH / 2, -height / 2, WIDTH, height);
		// Turning on antialiasing gives smoother-looking graphics
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));
		graphics.setColor(java.awt.Color.MAGENTA);
		setRotation(0.0);
	}

	/**
	 * Set this machine's rotation, in order to visually fall in line with the path.
	 *
	 * @param angle the angle to have this machine rotated
	 */
	public void setRotation(double angle) {
		// Rotation is the only time this machine's image will change, so update the image in this method
		graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
		AffineTransform saveAT = graphics.getTransform();
		// Rotate about and draw the image from the center of this object
		graphics.translate(image.getWidth() / 2, image.getHeight() / 2);
		graphics.rotate(angle);
		graphics.fill(shape);
		graphics.setTransform(saveAT);
	}

	public BufferedImage getImage() {
		return image;
	}
}
