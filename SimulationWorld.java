import greenfoot.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;

/**
 * ICS4U Vehicle Simulation
 *
 * @author Martin Baldwin
 */
public class SimulationWorld extends World {
	// Dimensions of this world
	public static final int WIDTH = 1024;
	public static final int HEIGHT = 768;

	public static final int PATH_WIDTH = 50;
	public static final java.awt.Color PATH_COLOR = new java.awt.Color(64, 64, 64);

	// Background image drawing facilities
	private BufferedImage canvas;
	private Graphics2D graphics;
	private Path2D.Double path;

	// Keep track of the last position of the mouse in order to control path curves
	private int prevMouseX;
	private int prevMouseY;

	/**
	 * Create a new simulation world.
	 */
	public SimulationWorld() {
		super(WIDTH, HEIGHT, 1, false);
		
		// Set up facilities to render graphics to background image
		GreenfootImage background = getBackground();
		canvas = background.getAwtImage();
		graphics = canvas.createGraphics();
		graphics.setStroke(new BasicStroke(PATH_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(PATH_COLOR);
		path = new Path2D.Double();
	}
	
	/**
	 * Update this world.
	 */
	public void act() {
		updateDrawing();
	}
	
	/**
	 * Update the background image of the world based on mouse events, allowing the user to draw.
	 */
	private void updateDrawing() {
		// Use position of mouse to add a point to the path
		MouseInfo mouse = Greenfoot.getMouseInfo();
		if (mouse == null) {
			return;
		}

		// When mouse changed from non-pressed to pressed state, begin a new path
		if (Greenfoot.mousePressed(null)) {
			path = new Path2D.Double();
			// Set starting point of path to initial mouse position
			path.moveTo(mouse.getX(), mouse.getY());
		} else if (mouse.getButton() != 0) {
			// Mouse is currently being dragged -> add a new point to the path
			// Use quadratic curves to smoothen the lines, connecting midpoints
			// of mouse positions with actual positions as control points
			int midx = (mouse.getX() + prevMouseX) / 2;
			int midy = (mouse.getY() + prevMouseY) / 2;
			path.quadTo(prevMouseX, prevMouseY, midx, midy);
		}
		graphics.draw(path);

		prevMouseX = mouse.getX();
		prevMouseY = mouse.getY();
	}
}
