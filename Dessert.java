import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * The "Vehicle" of the simulation.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class Dessert extends PathTraveller {
	private static final int TRUCK_BED_WIDTH = 32;
	private static final int TRUCK_BED_LENGTH = 64;
	private static final int PLATE_SIZE = TRUCK_BED_WIDTH - 5;

	private static final java.awt.Color TRUCK_BED_COLOR = new java.awt.Color(140, 140, 140);
	private static final java.awt.Color PLATE_COLOR = java.awt.Color.WHITE;

	public Dessert() {
		super(Truck.SPEED);
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
		graphics.fillRect(-TRUCK_BED_LENGTH, -TRUCK_BED_WIDTH / 2 , TRUCK_BED_LENGTH, TRUCK_BED_WIDTH);
		graphics.setColor(java.awt.Color.BLACK);
		graphics.drawRect(-TRUCK_BED_LENGTH, -TRUCK_BED_WIDTH / 2 , TRUCK_BED_LENGTH, TRUCK_BED_WIDTH);
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
}
