import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;

/**
 * A greedy wanderer that picks up any payload it can find underneath it as it
 * moves across the world.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Ufo extends Wanderer {
	public static final BufferedImage NORMAL_IMAGE = new GreenfootImage("images/ufo-normal.png").getAwtImage();
	public static final BufferedImage SCARED_IMAGE = new GreenfootImage("images/ufo-scared.png").getAwtImage();

	public static final int MIN_X = -NORMAL_IMAGE.getWidth();
	public static final int MAX_X = SimulationWorld.WIDTH + NORMAL_IMAGE.getWidth();
	public static final int MIN_Y = -NORMAL_IMAGE.getHeight();
	public static final int MAX_Y = SimulationWorld.HEIGHT + NORMAL_IMAGE.getHeight();

	public static final double MIN_SPEED = 2.0;
	public static final double MAX_SPEED = 3.0;
	public static final double SCARED_SPEED = 10.0;

	public static final double IMAGE_ROTATION_SPEED = Math.PI * 2.0 / 180.0;

	private static final Ellipse2D HIT_SHAPE = new Ellipse2D.Double(14, 14, 100, 100);

	// The angle at which this UFO is moving, which differs from its angle of rotation
	private double movementAngle;

	// The flag that controls whether this UFO will move in its usual spiral motion (false) or a straight line (true)
	private boolean isScared;
	// The image angle of this UFO at the time it was scared, for maintaining its image position after being scared
	private double scaredAngle;

	public Ufo() {
		super();

		// Initialize location and velocity
		double x1, y1, x2, y2;
		if ((int) (Math.random() * 2) == 0) {
			// Spawn on a horizontal edge of the world
			boolean top = (int) (Math.random() * 2) == 0;
			x1 = Math.random() * (MAX_X - MIN_X) + MIN_X;
			y1 = top ? MIN_Y : MAX_Y;
			// Move towards the opposite location across the world
			x2 = SimulationWorld.WIDTH - x1;
			y2 = top ? MAX_Y : MIN_Y;
		} else {
			// Spawn on a vertical edge of the world
			boolean left = (int) (Math.random() * 2) == 0;
			x1 = left ? MIN_X : MAX_X;
			y1 = Math.random() * (MAX_Y - MIN_Y) + MIN_Y;
			// Move towards the opposite location across the world
			x2 = left ? MAX_X : MIN_Y;
			y2 = SimulationWorld.HEIGHT - y1;
		}
		setLocation(x1, y1);
		setSpeed(Math.random() * (MAX_SPEED - MIN_SPEED) + MIN_SPEED);
		movementAngle = Math.atan2(y2 - y1, x2 - x1);
		isScared = false;
		scaredAngle = -1.0;
	}

	@Override
	public void act() {
		double imageAngle = getRotation();
		// Move according to this UFO's speed at its movement angle
		setRotation(movementAngle);
		super.act();

		// Die once this UFO has gone outside of the world
		if (getX() < MIN_X || getX() > MAX_X || getY() < MIN_Y || getY() > MAX_Y) {
			die();
			return;
		}

		// Rotate the image
		setRotation(imageAngle + IMAGE_ROTATION_SPEED);

		if (!isScared) {
			// Pick up any payloads on the path under this UFO
			SuperPath underPath = getWorld().getPathUnderPoint(getCenterX(), getCenterY());
			if (underPath == null) {
				return;
			}
			for (Payload payload : underPath.getTravellers(Payload.class)) {
				if (getHitShape().contains(payload.getItemX(), payload.getItemY())) {
					payload.removeItem();
				}
			}
		}
	}

	/**
	 * Return the X position of the center of this UFO's image.
	 */
	private double getCenterX() {
		return getX() - getImage().getWidth() / 2.0 * Math.cos(getRotation());
	}

	/**
	 * Return the Y position of the center of this UFO's image.
	 */
	private double getCenterY() {
		return getY() - getImage().getWidth() / 2.0 * Math.cos(getRotation());
	}

	/**
	 * Start making this UFO move in a straight lane at a faster speed directly
	 * away from the given point.
	 *
	 * @param x the x-coordinate of the point to move away from
	 * @param y the y-coordinate of the point to move away from
	 */
	public void scareAwayFromPoint(double x, double y) {
		isScared = true;
		movementAngle = Math.atan2(getY() - y, getX() - x);
		if (scaredAngle < 0.0) {
			scaredAngle = getRotation();
			setRotation(0.0);
		}
		setSpeed(SCARED_SPEED);
	}

	@Override
	public BufferedImage getImage() {
		if (isScared) {
			return SCARED_IMAGE;
		}
		return NORMAL_IMAGE;
	}

	@Override
	public AffineTransform getImageTransform() {
		BufferedImage image = getImage();
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		if (isScared) {
			// Restore the original image center point at the time of being scared
			transform.rotate(scaredAngle);
			transform.translate(-image.getWidth() / 2.0, 0.0);
		}
		// Rotate from the center if scared, midright point otherwise
		transform.rotate(getRotation());
		// Place image center at this UFO's location if scared, midright of image otherwise
		if (isScared) {
			transform.translate(-image.getWidth() / 2.0, -image.getHeight() / 2.0);
		} else {
			transform.translate(-image.getWidth(), -image.getHeight() / 2.0);
		}
		return transform;
	}

	@Override
	public Shape getHitShape() {
		return getImageTransform().createTransformedShape(HIT_SHAPE);
	}
}
