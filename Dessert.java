import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * The "Vehicle" of the simulation.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class Dessert extends PathTraveller {
	public static final int TRUCK_BED_WIDTH = 32;
	public static final int TRUCK_BED_LENGTH = 48;
	// The area of a dessert's truck bed, with its midright point at the origin
	private static final Rectangle2D TRUCK_BED_RECT = new Rectangle2D.Double(0, 0, TRUCK_BED_LENGTH, TRUCK_BED_WIDTH);

	private static final int PLATE_SIZE = TRUCK_BED_WIDTH - 5;

	private static final java.awt.Color TRUCK_BED_COLOR = new java.awt.Color(140, 140, 140);
	private static final java.awt.Color PLATE_COLOR = java.awt.Color.WHITE;

	private final BufferedImage image;
	private final Graphics2D graphics;

	// The truck actor that this dessert is following
	private Truck attachedTruck;

	/**
	 * Create a new dessert actor attached to the given truck.
	 *
	 * @param truck the truck actor to link this dessert to.
	 */
	public Dessert(Truck truck) {
		super(truck.getSpeed());
		linkActor(truck);
		attachedTruck = truck;

		// Initialize image
		image = GraphicsUtilities.createCompatibleTranslucentImage(TRUCK_BED_LENGTH, TRUCK_BED_WIDTH);
		graphics = image.createGraphics();
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);

		// Draw truck bed
		graphics.setColor(TRUCK_BED_COLOR);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		graphics.setColor(java.awt.Color.BLACK);
		graphics.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
		// Draw plate
		graphics.setColor(PLATE_COLOR);
		graphics.fillOval((TRUCK_BED_LENGTH - PLATE_SIZE) / 2, (TRUCK_BED_WIDTH - PLATE_SIZE) / 2, PLATE_SIZE, PLATE_SIZE);
		// Draw sprite
		BufferedImage sprite = getSprite();
		graphics.drawImage(sprite, (TRUCK_BED_LENGTH - sprite.getWidth()) / 2, (TRUCK_BED_WIDTH - sprite.getHeight()) / 2, null);
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * Retrieve the BufferedImage of the dessert sprite to draw on top of the
	 * truck bed and plate for this dessert's image.
	 */
	protected abstract BufferedImage getSprite();

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		return transform.createTransformedShape(TRUCK_BED_RECT);
	}
}
