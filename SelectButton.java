import greenfoot.*;

/**
 * A button with an icon that responds visually to user interaction and performs
 * an action when clicked. Buttons appear on top of the SimulationWorld (since
 * they are true Greenfoot Actors).
 *
 * @author Martin Baldwin
 */
public class SelectButton extends Actor {
	public static final int WIDTH = 64;
	public static final int HEIGHT = 64;

	private static final Color BORDER_COLOR = Color.BLACK;
	private static final int BORDER_WIDTH = 3;

	/**
	 * States that buttons may be in, with different background colors to match.
	 *
	 * NORMAL: This button is not being interacted with
	 * HOVER: The mouse cursor is on top of this button, but the mouse buttons are not pressed
	 * ACTIVE: The mouse cursor is on top of this button and a mouse button is currently pressed ("click")
	 * SELECTED: The button is marked as "selected" (state controlled by select() and deselect() methods)
	 */
	public enum ButtonState {
		NORMAL(new Color(224, 224, 224)),
		HOVER(new Color(200, 200, 200)),
		ACTIVE(new Color(176, 176, 176)),
		SELECTED(ButtonState.ACTIVE.backgroundColor);

		public final Color backgroundColor;

		private ButtonState(Color backgroundColor) {
			this.backgroundColor = backgroundColor;
		}
	}

	private GreenfootImage icon;
	private int iconX;
	private int iconY;
	private boolean isClicking;
	private boolean isSelected;

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
	public SelectButton(GreenfootImage icon, Callback callback, boolean isSelected) {
		super();
		this.icon = icon;
		iconX = WIDTH / 2 - icon.getWidth() / 2;
		iconY = HEIGHT / 2 - icon.getHeight() / 2;
		this.callback = callback;

		// Default button state
		isClicking = false;
		this.isSelected = isSelected;
		if (isSelected) {
			state = ButtonState.SELECTED;
		} else {
			state = ButtonState.NORMAL;
		}
		prevState = null;

		// Initialize image for this button actor
		image = new GreenfootImage(WIDTH, HEIGHT);
		setImage(image);
		updateImage();
	}

	/**
	 * Mark this button as selected. The button will be drawn with a darker background permanently until deselected.
	 */
	public void select() {
		isSelected = true;
	}

	/**
	 * Stop treating this button as selected. Background color will return to being determined by mouse interaction.
	 */
	public void deselect() {
		isSelected = false;
	}

	/**
	 * Update this button's image according to selection and mouse interaction and run the callback method if clicked.
	 */
	public void act() {
		if (isSelected) {
			state = ButtonState.SELECTED;
		} else {
			MouseInfo mouse = Greenfoot.getMouseInfo();
			if (mouse != null && isUnderPoint(mouse.getX(), mouse.getY())) {
				if (Greenfoot.mousePressed(this)) {
					// Mouse was just pressed on this button -> change to active state
					isClicking = true;
					state = ButtonState.ACTIVE;
				} else if (!isClicking) {
					state = ButtonState.HOVER;
				}
				// Run the callback method when the mouse button is released on this button
				if (Greenfoot.mouseClicked(this)) {
					callback.run();
					// End the active state (effect takes place in next if block)
					isClicking = false;
					// Callback could have selected this button
					if (isSelected) {
						state = ButtonState.SELECTED;
					} else {
						state = ButtonState.HOVER;
					}
				}
			} else {
				isClicking = false;
				state = ButtonState.NORMAL;
			}
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
		int x = getX() - WIDTH / 2;
		int y = getY() - HEIGHT / 2;
		return px >= x && px < x + WIDTH && py >= y && py < y + HEIGHT;
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
		image.fillRect(0, 0, WIDTH, BORDER_WIDTH);
		image.fillRect(0, HEIGHT - BORDER_WIDTH, WIDTH, BORDER_WIDTH);
		image.fillRect(0, 0, BORDER_WIDTH, HEIGHT);
		image.fillRect(WIDTH - BORDER_WIDTH, 0, BORDER_WIDTH, HEIGHT);
		// Draw button icon
		image.drawImage(icon, iconX, iconY);
	}
}
