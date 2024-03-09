import greenfoot.*;

/**
 * A widget consisting of an icon GreenfootImage on a fixed-size background
 * GreenfootImage.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Widget extends Actor {
	public static final int WIDTH = 64;
	public static final int HEIGHT = 64;

	private static final Color BACKGROUND_COLOR = Color.WHITE;

	// Image to draw on top of this widget
	private GreenfootImage icon;
	// Final image of this widget as an actor
	protected GreenfootImage image;

	/**
	 * Create a new widget using the given icon.
	 *
	 * @param icon the image to draw on top of the center of this widget, or null for no icon
	 */
	public Widget(GreenfootImage icon) {
		// Initialize image for this widget actor
		image = new GreenfootImage(WIDTH, HEIGHT);
		setIcon(icon);
		setImage(image);
	}

	/**
	 * Set this widget's icon to the given GreenfootImage, then update this
	 * widget's image.
	 *
	 * @param icon the image to use as this widget's icon
	 */
	public void setIcon(GreenfootImage icon) {
		this.icon = icon;
		updateImage();
	}

	/**
	 * Redraw this widget's image.
	 */
	protected void updateImage() {
		image.setColor(BACKGROUND_COLOR);
		image.fill();
		drawIcon();
	}

	/**
	 * Draw this widget's icon onto its image if it has an icon.
	 */
	protected void drawIcon() {
		if (icon == null) {
			return;
		}
		image.drawImage(icon, WIDTH / 2 - icon.getWidth() / 2, HEIGHT / 2 - icon.getHeight() / 2);
	}
}
