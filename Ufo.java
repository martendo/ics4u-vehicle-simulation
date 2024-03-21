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
	public static final BufferedImage IMAGE = new GreenfootImage("images/ufo.png").getAwtImage();

	public static final int MIN_X = -IMAGE.getWidth();
	public static final int MAX_X = SimulationWorld.WIDTH + IMAGE.getWidth();
	public static final int MIN_Y = -IMAGE.getHeight();
	public static final int MAX_Y = SimulationWorld.HEIGHT + IMAGE.getHeight();

	public static final double MIN_SPEED = 2.0;
	public static final double MAX_SPEED = 3.0;

	public static final double IMAGE_ROTATION_SPEED = Math.PI * 2.0 / 180.0;

	private static final Ellipse2D HIT_SHAPE = new Ellipse2D.Double(0, 0, IMAGE.getWidth(), IMAGE.getHeight());

	// The angle at which this UFO is moving, which differs from its angle of rotation
	private final double movementAngle;

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

		// Pick up any payloads under this UFO
		for (Payload payload : getWorld().getActors(Payload.class)) {
			if (getHitShape().contains(payload.getItemX(), payload.getItemY())) {
				payload.removeItem();
			}
		}
	}

	@Override
	public BufferedImage getImage() {
		return IMAGE;
	}

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		transform.translate(-IMAGE.getWidth(), -IMAGE.getHeight() / 2.0);
		return transform.createTransformedShape(HIT_SHAPE);
	}
}
