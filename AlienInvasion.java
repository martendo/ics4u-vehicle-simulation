import greenfoot.util.GraphicsUtilities;
import greenfoot.GreenfootSound;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Composite;
import java.awt.AlphaComposite;

/**
 * The worldwide effect of this project. The visual element consists of a
 * shrinking repeating ring pattern drawn over top of the entire world.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class AlienInvasion extends Effect {
	// Number of acts that an alien invasion lasts
	public static final int LIFESPAN = 600;

	// Amount to multiply the speeds of all path travellers during an invasion
	public static final double TRAVELLER_SPEED_FACTOR = 4.0;

	private static final java.awt.Color PATTERN_COLOR_1 = new java.awt.Color(124, 88, 182);
	private static final java.awt.Color PATTERN_COLOR_2 = new java.awt.Color(138, 105, 191);
	// Width of each segment of the visual pattern
	private static final int RING_WIDTH = 60;

	private static final GreenfootSound SOUND = new GreenfootSound("sounds/invasion.wav");
	private static boolean wasSoundPaused = false;

	// Whether or not there currently exists an AlienInvasion object
	private static boolean isActive;

	private final BufferedImage image;
	private final Graphics2D graphics;
	// The diameter of the circle to draw
	private final int size;
	// The offset of the circle from the topleft corner of the image
	private final int imageX;
	private final int imageY;

	// An additional spawner for spawning UFOs in the world more frequently
	private final Spawner ufoSpawner;
	// The offset of the positions of each ring
	private int offset;

	public AlienInvasion() {
		super(LIFESPAN);

		// Initialize image
		image = GraphicsUtilities.createCompatibleTranslucentImage(SimulationWorld.WIDTH, SimulationWorld.HEIGHT);
		graphics = image.createGraphics();
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));
		size = (int) (Math.hypot(image.getWidth(), image.getHeight()));
		imageX = image.getWidth() / 2 - size / 2;
		imageY = image.getHeight() / 2 - size / 2;

		offset = 0;
		ufoSpawner = new FixedSpawner(20) {
			@Override
			public void run() {
				getWorld().spawnUfo();
			}
		};

		setLocation(0, 0);

		// Signal that an alien invasion has begun
		isActive = true;
		SOUND.playLoop();
	}

	@Override
	public void addedToWorld(SimulationWorld world) {
		// Enact effects
		for (PathTraveller driver : world.getActors(Driver.class)) {
			driver.setSpeed(driver.getSpeed() * TRAVELLER_SPEED_FACTOR);
		}
		for (Bird bird : world.getActors(Bird.class)) {
			bird.scareAway();
		}
		world.addSpawner(ufoSpawner);
	}

	@Override
	public void die() {
		super.die();
		// Undo effects
		for (PathTraveller driver : getWorld().getActors(Driver.class)) {
			driver.setSpeed(driver.getSpeed() / TRAVELLER_SPEED_FACTOR);
		}
		getWorld().removeSpawner(ufoSpawner);
		SOUND.stop();
		isActive = false;
	}

	@Override
	public void act() {
		super.act();
		if (isDead()) {
			return;
		}

		// Update image
		graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
		graphics.setColor(offset >= RING_WIDTH ? PATTERN_COLOR_2 : PATTERN_COLOR_1);
		graphics.fillOval(imageX, imageY, size, size);
		for (int pos = offset % RING_WIDTH, i = offset >= RING_WIDTH ? 1 : 0; pos < size / 2; pos += RING_WIDTH, i++) {
			graphics.setColor((i & 1) == 0 ? PATTERN_COLOR_2 : PATTERN_COLOR_1);
			graphics.fillOval(imageX + pos, imageY + pos, size - pos * 2, size - pos * 2);
		}
		offset = (offset + 1) % (RING_WIDTH * 2);
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	@Override
	public AffineTransform getImageTransform() {
		// Don't transform this effect (covers entire world), return identity transform
		return new AffineTransform();
	}

	@Override
	public Composite getImageComposite() {
		return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
	}

	/**
	 * Test if there is currently an alien invasion.
	 */
	public static boolean isActive() {
		return isActive;
	}

	/**
	 * Pause the invasion sound if it is currently playing.
	 */
	public static void pauseSound() {
		wasSoundPaused = SOUND.isPlaying();
		SOUND.pause();
	}

	/**
	 * Resume the invasion sound if it was previously playing.
	 */
	public static void resumeSound() {
		if (wasSoundPaused) {
			SOUND.playLoop();
		}
	}
}
