import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Shape;

/**
 * The visual object representing the beginnings and ends of paths. They cover
 * up the round caps of SuperPaths as well as any path travellers that spawn or
 * die at the beginning and end points of paths.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Tunnel extends SuperActor {
	// The fixed length (or depth) of all tunnels
	private static final int ROOF_LENGTH = 32;
	// The difference between a tunnel's width and its roof's width
	private static final int ROOF_MARGIN = 24;
	private static final int POSITION_OFFSET = 64;

	private static final java.awt.Color BODY_COLOR = new java.awt.Color(196, 196, 196);
	private static final java.awt.Color ROOF_COLOR = new java.awt.Color(128, 128, 128, 128);

	private final BufferedImage image;
	private final Graphics2D graphics;

	// The dimensions of this tunnel, dependent on the width of its path
	private final int length;
	private final int width;
	// The path on which this tunnel appears
	private final SuperPath path;
	// Whether or not this tunnel should cover the path in the direction of its rotation (true) or in the direction opposite to its rotation (false)
	private final boolean forwards;

	/**
	 * Create a new tunnel actor.
	 *
	 * @param path the SuperPath that this tunnel belongs to
	 * @param forwards whether or not this tunnel should cover the path in the direction of its rotation (true) or in the direction opposite to its rotation (false)
	 */
	public Tunnel(SuperPath path, boolean forwards) {
		super();
		this.path = path;
		// Make sure to cover up the round cap of the path, taking into account the position offset
		length = path.getPathWidth() / 2 + SuperPath.PATH_OUTLINE_WIDTH + POSITION_OFFSET;
		// Hang over the edges of the path a little
		width = path.getPathWidth() + 50;
		this.forwards = forwards;

		// Initialize image
		int size = Math.max(length, width) * 2;
		image = GraphicsUtilities.createCompatibleTranslucentImage(size, size);
		graphics = image.createGraphics();
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));

		setRotation(0.0);
	}

	/**
	 * Set this tunnel's rotation, in order to visually fall in line with the path.
	 *
	 * @param angle the angle to have this tunnel rotated
	 */
	@Override
	public void setRotation(double angle) {
		super.setRotation(angle);
		// Rotation is the only time this tunnel's image will change, so update the image in this method
		updateImage();
	}

	public void updateImage() {
		graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
		AffineTransform saveTransform = graphics.getTransform();

		// Rotate about the center of this image
		graphics.translate((double) image.getWidth() / 2.0, (double) image.getHeight() / 2.0);
		graphics.rotate(getRotation());
		// Place this tunnel's midleft (if forwards) or midright (if backwards) point at the center of its image
		// Draw roof
		graphics.translate(forwards ? POSITION_OFFSET : -(ROOF_LENGTH + POSITION_OFFSET), -(width - ROOF_MARGIN) / 2);
		graphics.setColor(ROOF_COLOR);
		graphics.fillRect(0, 0, ROOF_LENGTH, (width - ROOF_MARGIN));
		graphics.setColor(java.awt.Color.BLACK);
		graphics.drawRect(0, 0, ROOF_LENGTH, (width - ROOF_MARGIN));
		// Draw body
		graphics.translate(forwards ? -length : ROOF_LENGTH, -ROOF_MARGIN / 2);
		graphics.setColor(BODY_COLOR);
		graphics.fillRect(0, 0, length, width);
		graphics.setColor(java.awt.Color.BLACK);
		graphics.drawRect(0, 0, length, width);

		graphics.setTransform(saveTransform);
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	@Override
	public AffineTransform getImageTransform() {
		return AffineTransform.getTranslateInstance(getX() - image.getWidth() / 2.0, getY() - image.getHeight() / 2.0);
	}

	@Override
	public Shape getHitShape() {
		throw new UnsupportedOperationException("Tunnel objects do not have hit shapes");
	}
}
