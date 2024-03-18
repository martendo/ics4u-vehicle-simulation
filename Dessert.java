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
	private static final int TRUCK_BED_WIDTH = 32;
	private static final int TRUCK_BED_LENGTH = 64;
	// The area of a dessert's truck bed, with its midright point at the origin
	private static final Rectangle2D TRUCK_BED_RECT = new Rectangle2D.Double(-TRUCK_BED_LENGTH, -TRUCK_BED_WIDTH / 2.0, TRUCK_BED_LENGTH, TRUCK_BED_WIDTH);

	private static final int PLATE_SIZE = TRUCK_BED_WIDTH - 5;

	private static final java.awt.Color TRUCK_BED_COLOR = new java.awt.Color(140, 140, 140);
	private static final java.awt.Color PLATE_COLOR = java.awt.Color.WHITE;

	/**
	 * Create a new dessert actor attached to the given truck.
	 *
	 * @param truck the truck actor to link this dessert to.
	 */
	public Dessert(Truck truck) {
		super(truck.getSpeed());
		linkActor(truck);
	}

	/**
	 * Draw the dessert's sprite, on top of a plate on top of a truck bed.
	 */
	@Override
	public void updateImage() {
		resetImage();
		BufferedImage image = getImage();
		BufferedImage sprite = getSprite();
		Graphics2D graphics = getGraphics();
		AffineTransform saveTransform = graphics.getTransform();

		AffineTransform transform = getCenterRotateTransform();
		graphics.transform(transform);
		// Draw truck bed
		graphics.setColor(TRUCK_BED_COLOR);
		graphics.fill(TRUCK_BED_RECT);
		graphics.setColor(java.awt.Color.BLACK);
		graphics.draw(TRUCK_BED_RECT);
		// Draw plate
		graphics.setColor(PLATE_COLOR);
		graphics.fillOval(-(TRUCK_BED_LENGTH + PLATE_SIZE) / 2, -PLATE_SIZE / 2, PLATE_SIZE, PLATE_SIZE);

		graphics.setTransform(saveTransform);

		drawSpriteToImage();
	}

	@Override
	protected AffineTransform getSpriteTransform() {
		BufferedImage sprite = getSprite();
		AffineTransform transform = getCenterRotateTransform();
		transform.translate(-(TRUCK_BED_LENGTH + sprite.getWidth()) / 2.0, -sprite.getHeight() / 2.0);
		return transform;
	}

	@Override
	protected int getImageSize() {
		return TRUCK_BED_LENGTH * 2;
	}

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		return transform.createTransformedShape(TRUCK_BED_RECT);
	}
}
