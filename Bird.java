import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

/**
 * A hungry wanderer that will look for food to fly towards and eat, or fly
 * randomly when there is none available or it has already been fed.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Bird extends Wanderer {
	public static final BufferedImage IMAGE = new GreenfootImage("images/bird.png").getAwtImage();

	private static final double SPEED = 4.0;

	private static final double MIN_X = -IMAGE.getWidth() / 2.0;
	private static final double MAX_X = SimulationWorld.WIDTH + IMAGE.getWidth() / 2.0;
	private static final double MIN_Y = -IMAGE.getHeight() / 2.0;
	private static final double MAX_Y = SimulationWorld.HEIGHT + IMAGE.getHeight() / 2.0;

	private static final double ANGLE_INTERPOLATION_FACTOR = 0.1;
	private static final double RANDOM_ANGLE_INTERPOLATION_FACTOR = 0.01;

	// The angle of rotation of this bird required to reach the closest food actor
	private double targetAngle;
	// The current target of this bird
	private Food targetFood;
	// Whether or not this bird is currently looking for food
	private boolean isFed;

	// Timer that will periodically change this bird's target angle randomly when there is no food available
	private Spawner randomTargetChangeTimer;

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
		randomTargetChangeTimer = new RandomSpawner(60, 120) {
			@Override
			public void run() {
				targetAngle = Math.random() * Math.PI * 2.0;
				optimizeRotation();
			}
		};
		isFed = false;
	}

	@Override
	public void addedToWorld(SimulationWorld world) {
		// Look for food in the new world
		findTargetFood();
	}

	/**
	 * Set this bird's target angle pointed towards the closest food actor.
	 */
	private void findTargetFood() {
		targetFood = null;
		if (getLayer() < 0) {
			return;
		}
		// Find the closest food actor that has its food item
		double minDistance = 0.0;
		for (Food food : getWorld().getActors(Food.class, getLayer())) {
			if (!food.hasItem()) {
				continue;
			}
			double distance = Math.hypot(food.getItemX() - getX(), food.getItemY() - getY());
			if (distance < minDistance || targetFood == null) {
				targetFood = food;
				minDistance = distance;
			}
		}
		// Target the food that was just found (or do nothing if null)
		updateTargetAngle();
	}

	/**
	 * Set this bird's target angle to the direction towards its target food.
	 * Do nothing when this bird is not targeting any food actor.
	 */
	private void updateTargetAngle() {
		if (targetFood == null) {
			return;
		}
		targetAngle = Math.atan2(targetFood.getItemY() - getY(), targetFood.getItemX() - getX());
		optimizeRotation();
	}

	/**
	 * Add or subtract a full revolution from this bird's current rotation in
	 * order to minimize the interpolation distance towards its target angle.
	 */
	private void optimizeRotation() {
		double angle = getRotation();
		// Interpolate angle towards targetAngle, wrapping around when one direction is closer than the current
		if (Math.abs(targetAngle - angle) > Math.abs(targetAngle - (angle - Math.PI * 2.0))) {
			angle -= Math.PI * 2.0;
		} else if (Math.abs(targetAngle - angle) > Math.abs(targetAngle - (angle + Math.PI * 2.0))) {
			angle += Math.PI * 2.0;
		}
		setRotation(angle);
	}

	@Override
	public void act() {
		// Find a new food actor to target when this bird doesn't have one already and it hasn't been fed yet
		if (!isFed && (targetFood == null || targetFood.isDead() || !targetFood.hasItem())) {
			findTargetFood();
		}
		// When there is no food available or this bird has already been fed, move randomly
		double t;
		if (targetFood == null || isFed) {
			randomTargetChangeTimer.act();
			t = RANDOM_ANGLE_INTERPOLATION_FACTOR;
		} else {
			updateTargetAngle();
			t = ANGLE_INTERPOLATION_FACTOR;
		}
		// Interpolate towards the target angle
		setRotation(getRotation() + (targetAngle - getRotation()) * t);

		// Move
		super.act();

		// When out of bounds of the world, kill this bird
		if (getX() < MIN_X || getX() > MAX_X || getY() < MIN_Y || getY() > MAX_Y) {
			die();
			return;
		} else if (targetFood != null && targetFood.getHitShape().contains(getX(), getY())) {
			// This bird has reached its target food -> eat it
			targetFood.removeItem();
			isFed = true;
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
