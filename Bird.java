import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

public class Bird extends Wanderer {
	public static final BufferedImage IMAGE = new GreenfootImage("images/bird.png").getAwtImage();

	private static final double SPEED = 4.0;

	private static final double MIN_X = -IMAGE.getWidth() / 2.0;
	private static final double MAX_X = SimulationWorld.WIDTH + IMAGE.getWidth() / 2.0;
	private static final double MIN_Y = -IMAGE.getHeight() / 2.0;
	private static final double MAX_Y = SimulationWorld.HEIGHT + IMAGE.getHeight() / 2.0;

	// The angle of rotation of this bird at the start of a lerp interval
	private double prevAngle;
	// The angle of rotation of this bird at the end of a lerp interval
	private double targetAngle;
	// The current angle interpolation factor ("t"), in [0.0, 1.0]
	private double lerp;

	// Timer that will periodically change this bird's target angle
	private Spawner targetChangeTimer;

	public Bird() {
		super();

		// Initialize location
		double x, y;
		if ((int) (Math.random() * 2) == 0) {
			// Spawn on a horizontal edge of the world
			boolean top = (int) (Math.random() * 2) == 0;
			x = Math.random() * (MAX_X - MIN_X) + MIN_X;
			y = top ? MIN_Y : MAX_Y;
		} else {
			// Spawn on a vertical edge of the world
			boolean left = (int) (Math.random() * 2) == 0;
			x = left ? MIN_X : MAX_X;
			y = Math.random() * (MAX_Y - MIN_Y) + MIN_Y;
		}
		setLocation(x, y);

		setSpeed(SPEED);
		setRotation(Math.atan2(SimulationWorld.HEIGHT / 2.0 - y, SimulationWorld.WIDTH / 2.0 - x));
		targetChangeTimer = new RandomSpawner(60, 120) {
			@Override
			public void run() {
				updateTarget();
			}
		};
		updateTarget();
	}

	/**
	 * Set up this bird for a new target angle to interpolate towards.
	 */
	private void updateTarget() {
		prevAngle = getRotation();
		targetAngle = Math.random() * Math.PI * 2.0;
		if (Math.abs(targetAngle - Math.PI * 2.0 - prevAngle) < Math.abs(targetAngle - prevAngle)) {
			targetAngle -= Math.PI * 2.0;
		}
		lerp = 0.0;
	}

	@Override
	public void act() {
		targetChangeTimer.act();
		if (lerp < 1.0) {
			lerp += 0.005;
		}
		setRotation(prevAngle + (targetAngle - prevAngle) * lerp);

		setLocation(getX() + getSpeed() * Math.cos(getRotation()), getY() + getSpeed() * Math.sin(getRotation()));
		if (getX() < MIN_X || getX() > MAX_X || getY() < MIN_Y || getY() > MAX_Y) {
			die();
		}
	}

	@Override
	public BufferedImage getImage() {
		return IMAGE;
	}

	@Override
	public AffineTransform getImageTransform() {
		// Draw the image centered at and rotated around this bird's location
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		transform.translate(-IMAGE.getWidth() / 2.0, -IMAGE.getHeight() / 2.0);
		return transform;
	}
}
