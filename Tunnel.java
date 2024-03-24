import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The visual object representing the beginnings and ends of paths. They cover
 * up the round caps of SuperPaths as well as any path travellers that spawn or
 * die at the beginning and end points of paths.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Tunnel extends SuperActor {
	// Settings
	public static final boolean DEBUG_SHOW_BOUNDING_RECT = false;

	// The fixed length (or depth) of all tunnels
	private static final int ROOF_LENGTH = 32;
	// The difference between a tunnel's width and its roof's width
	private static final int ROOF_MARGIN = 24;
	private static final int POSITION_OFFSET = 64;

	private static final java.awt.Color BODY_COLOR = new java.awt.Color(208, 122, 74);
	private static final java.awt.Color ROOF_COLOR = new java.awt.Color(128, 128, 128, 128);
	private static final java.awt.Color BORDER_COLOR = new java.awt.Color(32, 32, 32);

	private static final BasicStroke BORDER_STROKE = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	// The rectangle containing this entire tunnel, before translation and rotation, for image cropping
	private final Rectangle2D boundsShape;

	// The image that is drawn onto, uncropped
	private final BufferedImage fullImage;
	private final Graphics2D graphics;
	// Cropped subimage of fullImage for faster drawing
	private BufferedImage croppedImage;
	// Rectangle defining the area relative to fullImage that croppedImage contains
	private Rectangle2D crop;

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
		boundsShape = createBoundsShape();

		// Initialize image
		int size = Math.max(length, width) * 2 + 8; // + Padding
		fullImage = GraphicsUtilities.createCompatibleTranslucentImage(size, size);
		croppedImage = fullImage;
		graphics = fullImage.createGraphics();
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);
		if (DEBUG_SHOW_BOUNDING_RECT) {
			graphics.setBackground(new java.awt.Color(0, 0, 128, 128));
		} else {
			graphics.setBackground(new java.awt.Color(0, 0, 0, 0));
		}
		graphics.setStroke(BORDER_STROKE);
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
		graphics.clearRect(0, 0, fullImage.getWidth(), fullImage.getHeight());
		AffineTransform saveTransform = graphics.getTransform();

		// Rotate about the center of this image
		graphics.transform(getDrawingTransform());
		// Place this tunnel's midleft (if forwards) or midright (if backwards) point at the center of its image
		// Draw roof
		graphics.translate(forwards ? POSITION_OFFSET : -(ROOF_LENGTH + POSITION_OFFSET), -(width - ROOF_MARGIN) / 2);
		graphics.setColor(ROOF_COLOR);
		graphics.fillRect(0, 0, ROOF_LENGTH, (width - ROOF_MARGIN));
		graphics.setColor(BORDER_COLOR);
		graphics.drawRect(0, 0, ROOF_LENGTH, (width - ROOF_MARGIN));
		// Draw body
		graphics.translate(forwards ? -length : ROOF_LENGTH, -ROOF_MARGIN / 2);
		graphics.setColor(BODY_COLOR);
		graphics.fillRect(0, 0, length, width);
		graphics.setColor(BORDER_COLOR);
		graphics.drawRect(0, 0, length, width);

		graphics.setTransform(saveTransform);

		// Crop this tunnel's image to its boundaries for faster drawing
		crop = getDrawingTransform().createTransformedShape(boundsShape).getBounds2D();
		// Grow bounds to include rendered subpixels
		crop.setRect(crop.getX() - 3, crop.getY() - 3, crop.getWidth() + 6, crop.getHeight() + 6);
		// Clamp bounds to original image dimensions
		crop = crop.createIntersection(new Rectangle2D.Double(0, 0, fullImage.getWidth(), fullImage.getHeight()));
		// Draw using cropped image (graphics continues to draw on full-size image, coordinates unaffected)
		croppedImage = fullImage.getSubimage((int) crop.getX(), (int) crop.getY(), (int) crop.getWidth(), (int) crop.getHeight());
	}

	@Override
	public BufferedImage getImage() {
		return croppedImage;
	}

	/**
	 * Get the transformation required to draw this tunnel onto its full image.
	 */
	private AffineTransform getDrawingTransform() {
		AffineTransform transform = AffineTransform.getTranslateInstance((double) fullImage.getWidth() / 2.0, (double) fullImage.getHeight() / 2.0);
		transform.rotate(getRotation());
		return transform;
	}

	/**
	 * Create the rectangle that contains this entire tunnel, before translation
	 * and rotation, for image cropping.
	 */
	private Rectangle2D createBoundsShape() {
		Rectangle2D bounds = new Rectangle2D.Double();
		if (forwards) {
			bounds.setRect(-length + POSITION_OFFSET, -width / 2.0, length + ROOF_LENGTH, width);
		} else {
			bounds.setRect(-(ROOF_LENGTH + POSITION_OFFSET), -width / 2.0, length + ROOF_LENGTH, width);
		}
		return bounds;
	}

	@Override
	public AffineTransform getImageTransform() {
		return AffineTransform.getTranslateInstance(getX() - fullImage.getWidth() / 2.0 + crop.getX(), getY() - fullImage.getHeight() / 2.0 + crop.getY());
	}
}
