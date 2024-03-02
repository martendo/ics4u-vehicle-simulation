import greenfoot.Greenfoot;
import greenfoot.MouseInfo;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Rectangle;

public class Button extends SuperActor {
	private static final java.awt.Color BORDER_COLOR = java.awt.Color.BLACK;
	private static final BasicStroke BORDER_STROKE = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	/**
	 * States that buttons may be in, with different background colors to match.
	 *
	 * NORMAL: This button is not being interacted with
	 * HOVER: The mouse cursor is on top of this button, but the mouse buttons are not pressed
	 * ACTIVE: The mouse curosr is on top of this button and a mouse button is currently pressed ("click")
	 */
	public enum ButtonState {
		NORMAL(new java.awt.Color(224, 224, 224)),
		HOVER(new java.awt.Color(200, 200, 200)),
		ACTIVE(new java.awt.Color(176, 176, 176));

		public final java.awt.Color backgroundColor;

		private ButtonState(java.awt.Color backgroundColor) {
			this.backgroundColor = backgroundColor;
		}
	}

	private BufferedImage icon;
	private Rectangle boundingBox;
	private int iconX;
	private int iconY;

	// Keep track of button state for drawing
	private ButtonState state;
	// The action to perform when this button is clicked
	private Callback callback;

	/**
	 * Create a new button.
	 *
	 * @param x the x-coordinate of the left side of the button
	 * @param y the y-coordinate of the top side of the button
	 * @param w the width of the button
	 * @param h the height of the button
	 * @param iconFile the filename of the image to place in the center of this button
	 * @param callback the action to perform when this button is clicked
	 * @throws IOException if an error occurs during reading the icon file
	 */
	public Button(int x, int y, int w, int h, String iconFile, Callback callback) throws IOException {
		super();
		boundingBox = new Rectangle(x, y, w, h);
		icon = ImageIO.read(new File(iconFile));
		iconX = x + (w / 2) - (icon.getWidth() / 2);
		iconY = y + (h / 2) - (icon.getHeight() / 2);
		state = ButtonState.NORMAL;
		this.callback = callback;
	}

	public void act() {
		MouseInfo mouse = Greenfoot.getMouseInfo();
		if (mouse != null && boundingBox.contains(mouse.getX(), mouse.getY())) {
			if (Greenfoot.mouseClicked(null)) {
				callback.run();
			}
			// Update button state for drawing
			if (mouse.getButton() != 0) {
				state = ButtonState.ACTIVE;
			} else {
				state = ButtonState.HOVER;
			}
		} else {
			state = ButtonState.NORMAL;
		}
	}

	public void drawUsingGraphics(Graphics2D graphics) {
		// Fill background of button
		graphics.setColor(state.backgroundColor);
		graphics.fill(boundingBox);
		// Draw border of button
		graphics.setColor(BORDER_COLOR);
		graphics.setStroke(BORDER_STROKE);
		graphics.draw(boundingBox);
		// Draw button icon
		graphics.drawImage(icon, iconX, iconY, null);
	}
}
