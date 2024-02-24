import greenfoot.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.util.ArrayList;

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
	private ArrayList<Path2D.Double> paths;

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
		// Normalize strokes to avoid strange visual artifacts in specific scenarios
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		// Turning on antialiasing gives smoother-looking graphics
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(new BasicStroke(PATH_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setBackground(new java.awt.Color(0, true));
		graphics.setColor(PATH_COLOR);

		paths = new ArrayList<Path2D.Double>();
	}

	/**
	 * Update this world.
	 */
	public void act() {
		updatePaths();
		updateBackground();
	}

	/**
	 * Update the paths in this world based on mouse events, allowing the user to draw.
	 */
	private void updatePaths() {
		// Use position of mouse to add a point to the path
		MouseInfo mouse = Greenfoot.getMouseInfo();
		if (mouse == null) {
			return;
		}

		if (Greenfoot.mousePressed(null)) {
			// When mouse changed from non-pressed to pressed state, begin a new path
			Path2D.Double path = new Path2D.Double();
			// Set starting point of path to initial mouse position
			path.moveTo(mouse.getX(), mouse.getY());
			paths.add(path);
		} else if (mouse.getButton() != 0 && paths.size() > 0) {
			// Mouse is currently being dragged -> add a new point to the current path
			Path2D.Double path = paths.get(paths.size() - 1);
			// Use quadratic curves to smoothen the lines, connecting midpoints
			// of mouse positions with actual positions as control points
			int midx = (mouse.getX() + prevMouseX) / 2;
			int midy = (mouse.getY() + prevMouseY) / 2;
			path.quadTo(prevMouseX, prevMouseY, midx, midy);
		}

		prevMouseX = mouse.getX();
		prevMouseY = mouse.getY();
	}

	/**
	 * Update this world's background image.
	 */
	private void updateBackground() {
		graphics.clearRect(0, 0, WIDTH, HEIGHT);
		for (Path2D.Double path : paths) {
			graphics.draw(path);
		}
	}
}
