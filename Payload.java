import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * The "Vehicle" of the simulation.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class Payload extends PathTraveller {
	public static final int TRUCK_BED_WIDTH = 32;
	public static final int TRUCK_BED_LENGTH = 48;

	// The area of a truck's bed, with its midright point at the origin
	private static final Shape HIT_SHAPE = new Rectangle2D.Double(-TRUCK_BED_LENGTH, -TRUCK_BED_WIDTH / 2.0, TRUCK_BED_LENGTH, TRUCK_BED_WIDTH);

	private static final int PLATE_SIZE = TRUCK_BED_WIDTH;

	private static final java.awt.Color TRUCK_BED_COLOR = new java.awt.Color(140, 140, 140);
	private static final java.awt.Color PLATE_COLOR = java.awt.Color.WHITE;
	private static final java.awt.Color BORDER_COLOR = java.awt.Color.BLACK;

	private static final BasicStroke BORDER_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private final BufferedImage image;
	private final Graphics2D graphics;

	// The truck actor that this payload is following
	private Truck attachedTruck;
	// Whether or not this payload is still carrying its item
	private boolean hasItem;

	/**
	 * Create a new payload actor attached to the given truck.
	 *
	 * @param truck the truck actor to link this payload to.
	 */
	public Payload(Truck truck) {
		super(truck.getSpeed());
		linkActor(truck);
		attachedTruck = truck;
		hasItem = true;

		// Initialize image
		image = GraphicsUtilities.createCompatibleTranslucentImage(TRUCK_BED_LENGTH + 8, TRUCK_BED_WIDTH + 8);
		graphics = image.createGraphics();
		graphics.addRenderingHints(SimulationWorld.renderingHints);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));
		graphics.translate(4, 4); // Padding
		graphics.setStroke(BORDER_STROKE);
		drawImage();
	}

	@Override
	public void addedToPath(SuperPath path, int laneNum) {
		super.addedToPath(path, laneNum);
		// In case truck has since sped up, fix position
		double dist = attachedTruck.getDistanceTravelled() - attachedTruck.getImage().getWidth();
		if (dist > 0.0) {
			// Move to the correct position
			setSpeed(dist);
			super.act();
			setSpeed(attachedTruck.getSpeed());
		}
	}


	public void drawImage() {
		graphics.clearRect(0, 0, image.getWidth(), image.getHeight());

		// Draw truck bed
		graphics.setColor(TRUCK_BED_COLOR);
		graphics.fillRect(0, 0, TRUCK_BED_LENGTH, TRUCK_BED_WIDTH);
		graphics.setColor(BORDER_COLOR);
		graphics.drawRect(0, 0, TRUCK_BED_LENGTH, TRUCK_BED_WIDTH);

		if (!hasItem) {
			return;
		}

		// Draw plate
		graphics.setColor(PLATE_COLOR);
		graphics.fillOval((TRUCK_BED_LENGTH - PLATE_SIZE) / 2, (TRUCK_BED_WIDTH - PLATE_SIZE) / 2, PLATE_SIZE, PLATE_SIZE);
		// Draw sprite
		BufferedImage sprite = getSprite();
		// Rotate sprite to always stay upright
		AffineTransform spriteTransform = AffineTransform.getTranslateInstance(TRUCK_BED_LENGTH / 2.0, TRUCK_BED_WIDTH / 2.0);
		spriteTransform.rotate(-getRotation());
		spriteTransform.translate(-sprite.getWidth() / 2.0, -sprite.getHeight() / 2.0);
		graphics.drawImage(sprite, spriteTransform, null);
	}

	@Override
	public void act() {
		// If this payload has travelled too far (e.g. truck spawned with a
		// higher speed than when the payload spawner went off), wait
		if (!attachedTruck.isDead() && getDistanceTravelled() > attachedTruck.getDistanceTravelled() - attachedTruck.getImage().getWidth()) {
			return;
		}
		super.act();
		// Update image to reflect angle change
		drawImage();
	}

	/**
	 * Test if this payload still has its item.
	 */
	public boolean hasItem() {
		return hasItem;
	}

	/**
	 * Remove the item from this payload.
	 */
	public void removeItem() {
		hasItem = false;
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * Get the X coordinate of the center of this payload actor.
	 */
	public double getItemX() {
		return getX() - (TRUCK_BED_LENGTH / 2.0) * Math.cos(getRotation());
	}

	/**
	 * Get the Y coordinate of the center of this payload actor.
	 */
	public double getItemY() {
		return getY() - (TRUCK_BED_WIDTH / 2.0) * Math.sin(getRotation());
	}

	/**
	 * Retrieve the BufferedImage of the payload sprite to draw on top of the
	 * truck bed and plate for this payload's image.
	 */
	protected abstract BufferedImage getSprite();

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		return transform.createTransformedShape(HIT_SHAPE);
	}
}
