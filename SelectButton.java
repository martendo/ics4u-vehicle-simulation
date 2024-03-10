import greenfoot.*;

/**
 * A button with an icon that responds visually to user interaction and performs
 * an action when clicked. Buttons appear on top of the SimulationWorld (since
 * they are true Greenfoot Actors).
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class SelectButton extends Widget {
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

	private boolean isClicking;
	private boolean isSelected;

	// Keep track of button state for drawing
	private ButtonState state;
	private ButtonState prevState;

	/**
	 * Create a new button.
	 *
	 * @param icon the image to place in the center of this button
	 * @param isSelected whether or not this button should begin in a selected state
	 */
	public SelectButton(GreenfootImage icon, boolean isSelected) {
		super(icon);

		// Default button state
		isClicking = false;
		this.isSelected = isSelected;
		if (isSelected) {
			state = ButtonState.SELECTED;
		} else {
			state = ButtonState.NORMAL;
		}
		prevState = null;
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
					if (isClicking) {
						clicked();
						// End the active state (effect takes place in next if block)
						isClicking = false;
					}
					// clicked() method could have selected this button, update state to reflect the change
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
	 * This method is called when the button is clicked.
	 */
	public abstract void clicked();

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
	protected void updateImage() {
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
		drawIcon();
	}
}
