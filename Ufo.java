import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;

/**
 * A greedy wanderer that picks up any payload it can find underneath it as it
 * moves across the world.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Ufo extends Wanderer {
	public static final BufferedImage IMAGE = new GreenfootImage("images/ufo.png").getAwtImage();

	public static final int MIN_SPAWN_X = -IMAGE.getWidth();
	public static final int MAX_SPAWN_X = SimulationWorld.WIDTH + IMAGE.getWidth();
	public static final int MIN_SPAWN_Y = -IMAGE.getHeight();
	public static final int MAX_SPAWN_Y = SimulationWorld.HEIGHT + IMAGE.getHeight();

	public static final double MIN_SPEED = 2.0;
	public static final double MAX_SPEED = 3.0;

	public static final double IMAGE_ROTATION_SPEED = Math.PI * 2.0 / 180.0;

	// The angle at which this UFO is moving, which differs from its angle of rotation
	private final double movementAngle;

	public Ufo() {
		super();

		// Initialize location and velocity
		double x1, y1, x2, y2;
		if ((int) (Math.random() * 2) == 0) {
			// Spawn on a horizontal edge of the world
			boolean top = (int) (Math.random() * 2) == 0;
			x1 = Math.random() * (MAX_SPAWN_X - MIN_SPAWN_X) + MIN_SPAWN_X;
			y1 = top ? MIN_SPAWN_Y : MAX_SPAWN_Y;
			// Move towards the opposite location across the world
			x2 = SimulationWorld.WIDTH - x1;
			y2 = top ? MAX_SPAWN_Y : MIN_SPAWN_Y;
		} else {
			// Spawn on a vertical edge of the world
			boolean left = (int) (Math.random() * 2) == 0;
			x1 = left ? MIN_SPAWN_X : MAX_SPAWN_X;
			y1 = Math.random() * (MAX_SPAWN_Y - MIN_SPAWN_Y) + MIN_SPAWN_Y;
			// Move towards the opposite location across the world
			x2 = left ? MAX_SPAWN_X : MIN_SPAWN_Y;
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

		// Rotate the image
		setRotation(imageAngle + IMAGE_ROTATION_SPEED);
	}

	@Override
	public BufferedImage getImage() {
		return IMAGE;
	}
}
