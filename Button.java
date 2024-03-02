import greenfoot.*;

/**
 * A button with an icon that responds visually to user interaction and performs
 * an action when clicked. Buttons appear on top of the SimulationWorld (since
 * they are true Greenfoot Actors).
 *
 * @author Martin Baldwin
 */
public class Button extends Actor {
	private static final Color BORDER_COLOR = Color.BLACK;
	private static final int BORDER_WIDTH = 3;

	/**
	 * States that buttons may be in, with different background colors to match.
	 *
	 * NORMAL: This button is not being interacted with
	 * HOVER: The mouse cursor is on top of this button, but the mouse buttons are not pressed
	 * ACTIVE: The mouse cursor is on top of this button and a mouse button is currently pressed ("click")
	 */
	public enum ButtonState {
		NORMAL(new Color(224, 224, 224)),
		HOVER(new Color(200, 200, 200)),
		ACTIVE(new Color(176, 176, 176));

		public final Color backgroundColor;

		private ButtonState(Color backgroundColor) {
			this.backgroundColor = backgroundColor;
		}
	}

	private int width;
	private int height;
	private GreenfootImage icon;
	private int iconX;
	private int iconY;
	private boolean isClicking;

	// Final image of this button as an actor
	private GreenfootImage image;

	// Keep track of button state for drawing
	private ButtonState state;
	private ButtonState prevState;
	// The action to perform when this button is clicked
	private Callback callback;

	/**
	 * Create a new button.
	 *
	 * @param width the width of the button
	 * @param height the height of the button
	 * @param icon the image to place in the center of this button
	 * @param callback the action to perform when this button is clicked
	 */
	public Button(int width, int height, GreenfootImage icon, Callback callback) {
		super();
		this.width = width;
		this.height = height;
		this.icon = icon;
		iconX = width / 2 - icon.getWidth() / 2;
		iconY = height / 2 - icon.getHeight() / 2;

		// Default button state
		state = ButtonState.NORMAL;
		prevState = null;
		this.callback = callback;
		isClicking = false;

		// Initialize image for this button actor
		image = new GreenfootImage(width, height);
		setImage(image);
		updateImage();
	}

	/**
	 * Update this button's image according to mouse interaction and run the callback method if clicked.
	 */
	public void act() {
		MouseInfo mouse = Greenfoot.getMouseInfo();
		if (mouse != null && isUnderPoint(mouse.getX(), mouse.getY())) {
			// Run the callback method when the mouse button is released on this button
			if (Greenfoot.mouseClicked(this)) {
				callback.run();
				// End the active state (effect takes place in next if block)
				isClicking = false;
			}
			if (Greenfoot.mousePressed(this)) {
				// Mouse was just pressed -> change to active state
				isClicking = true;
				state = ButtonState.ACTIVE;
			} else if (!isClicking) {
				state = ButtonState.HOVER;
			}
		} else {
			isClicking = false;
			state = ButtonState.NORMAL;
		}
		updateImage();
		prevState = state;
	}

	/**
	 * Test if the given point is contained within the boundaries of this button.
	 *
	 * @param px the x-coordinate of the point to test
	 * @param py the y-coordinate of the point to test
	 * @return true if (px, py) lies on top of this button, false otherwise
	 */
	private boolean isUnderPoint(int px, int py) {
		int x = getX() - width / 2;
		int y = getY() - height / 2;
		return px >= x && px < x + width && py >= y && py < y + height;
	}

	/**
	 * Redraw this button's image with the appropriate background color if this button's state has changed.
	 */
	private void updateImage() {
		if (state == prevState) {
			// State has not changed: image will be the same as before, nothing to do
			return;
		}
		// Fill background of button
		image.setColor(state.backgroundColor);
		image.fill();
		// Draw border of button
		image.setColor(BORDER_COLOR);
		image.fillRect(0, 0, width, BORDER_WIDTH);
		image.fillRect(0, height - BORDER_WIDTH, width, BORDER_WIDTH);
		image.fillRect(0, 0, BORDER_WIDTH, height);
		image.fillRect(width - BORDER_WIDTH, 0, BORDER_WIDTH, height);
		// Draw button icon
		image.drawImage(icon, iconX, iconY);
	}
}
