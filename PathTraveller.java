import java.awt.image.BufferedImage;
import greenfoot.util.GraphicsUtilities;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * An object that travels along a path at a constant speed, rotating to reflect
 * its journey along the path.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class PathTraveller extends SuperActor {
	private static final double ANGLE_INTERPOLATION_FACTOR = 0.1;

	private SuperPath path = null;
	// The iterator used to progressively travel along this traveller's path
	private PathTraceIterator pathIter = null;

	// Distance to travel along a path per act
	private double speed;

	// The exact angle of the current segment of the path
	private double targetAngle = 0.0;
	// The visually pleasing interpolated angle of this traveller, constantly moving towards targetAngle
	protected double angle = 0.0;

	private final BufferedImage image;
	private final Graphics2D graphics;

	public PathTraveller(double speed) {
		super();
		this.speed = speed;
		BufferedImage sprite = getSprite();
		int size = getImageSize();
		image = GraphicsUtilities.createCompatibleTranslucentImage(size, size);
		graphics = image.createGraphics();
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));
	}

	/**
	 * Get the current rotation of this traveller.
	 */
	public double getAngle() {
		return angle;
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
		// Start with average angle of path's beginning
		targetAngle = path.getStartAngle();
		angle = targetAngle;
		// Reflect angle change in image
		updateImage();
	}

	/**
	 * Move this traveller along its path, and
	 */
	@Override
	public void act() {
		// Move along the path a distance equal to traveller speed
		pathIter.next(speed);
		if (pathIter.isDone()) {
			endTravel();
			return;
		}
		// Set this traveller's location to the end point of the iterator's current line segment
		double[] coords = new double[6];
		pathIter.currentSegment(coords);
		// Target angle between previous and new position
		targetAngle = Math.atan2(coords[1] - y, coords[0] - x);
		// Interpolate angle towards targetAngle, wrapping around when one direction is closer than the current
		if (Math.abs(targetAngle - angle) > Math.abs(targetAngle - (angle - Math.PI * 2.0))) {
			angle -= Math.PI * 2.0;
		} else if (Math.abs(targetAngle - angle) > Math.abs(targetAngle - (angle + Math.PI * 2.0))) {
			angle += Math.PI * 2.0;
		}
		angle += (targetAngle - angle) * ANGLE_INTERPOLATION_FACTOR;

		// Update position
		x = coords[0];
		y = coords[1];

		// Reflect angle change in image
		updateImage();
	}

	/**
	 * Kill this traveller and remove it from its path.
	 */
	@Override
	public void die() {
		super.die();
		path.removeTraveller(this);
	}

	/**
	 * This method is called when this traveller reaches the end of its path.
	 *
	 * By default, simply kill this traveller.
	 */
	protected void endTravel() {
		die();
	}

	/**
	 * Return this traveller's graphics context.
	 */
	protected Graphics2D getGraphics() {
		return graphics;
	}

	/**
	 * Retrieve the image to draw rotated for this traveller's image.
	 */
	protected abstract BufferedImage getSprite();

	protected int getImageSize() {
		BufferedImage sprite = getSprite();
		return Math.max(sprite.getWidth(), sprite.getHeight()) * 2;
	}

	/**
	 * Set this traveller's image to its sprite image rotated to its current angle.
	 */
	private void updateImage() {
		BufferedImage sprite = getSprite();
		resetImage();
		// Rotate from the center of this traveller's image
		AffineTransform transform = AffineTransform.getTranslateInstance(image.getWidth() / 2, image.getHeight() / 2);
		transform.rotate(angle);
		// Place this traveller's sprite so its midright point is at the center of the image
		transform.translate(-sprite.getWidth(), -sprite.getHeight() / 2);
		graphics.drawImage(sprite, transform, null);
	}

	/**
	 * Prepare this traveller's image for drawing.
	 */
	protected void resetImage() {
		graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}
}
