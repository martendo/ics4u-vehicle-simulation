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

	private static final int BACKGROUND_PATTERN_WIDTH = 128;
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_1 = new java.awt.Color(255, 200, 155);
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_2 = new java.awt.Color(255, 190, 140);

	// Background image drawing facilities
	private BufferedImage canvas;
	private Graphics2D graphics;
	private ArrayList<Path2D.Double> paths;

	// Keep track of the last position of the mouse in order to control path curves
	private int prevMouseX;
	private int prevMouseY;

	// Animate the background pattern by shifting it horizontally
	private int patternShift = 0;

	/**
	 * Create a new simulation world.
	 */
	public SimulationWorld() {
		super(WIDTH, HEIGHT, 1, false);

		Greenfoot.setSpeed(50);

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

		paths = new ArrayList<Path2D.Double>();

		// Draw initial background image so this world isn't blank on reset
		updateBackground();
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

		// Draw background pattern
		graphics.setColor(BACKGROUND_PATTERN_COLOR_1);
		for (int x1 = -patternShift; x1 < WIDTH + HEIGHT; x1 += BACKGROUND_PATTERN_WIDTH * 2) {
			int x2 = x1 + BACKGROUND_PATTERN_WIDTH;
			int x3 = x2 - HEIGHT;
			int x4 = x1 - HEIGHT;
			graphics.fillPolygon(new int[] {x1, x2, x3, x4}, new int[] {0, 0, HEIGHT, HEIGHT}, 4);
		}
		graphics.setColor(BACKGROUND_PATTERN_COLOR_2);
		for (int x1 = BACKGROUND_PATTERN_WIDTH - patternShift; x1 < WIDTH + HEIGHT; x1 += BACKGROUND_PATTERN_WIDTH * 2) {
			int x2 = x1 + BACKGROUND_PATTERN_WIDTH;
			int x3 = x2 - HEIGHT;
			int x4 = x1 - HEIGHT;
			graphics.fillPolygon(new int[] {x1, x2, x3, x4}, new int[] {0, 0, HEIGHT, HEIGHT}, 4);
		}
		// Shift the background pattern for the next act
		patternShift = (patternShift + 1) % (BACKGROUND_PATTERN_WIDTH * 2);

		// Draw paths
		graphics.setColor(PATH_COLOR);
		for (Path2D.Double path : paths) {
			graphics.draw(path);
		}
	}
}
