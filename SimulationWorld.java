import greenfoot.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * ICS4U Vehicle Simulation
 *
 * @author Martin Baldwin
 */
public class SimulationWorld extends World {
	// Dimensions of this world
	public static final int WIDTH = 1024;
	public static final int HEIGHT = 768;

	private static final int BACKGROUND_PATTERN_WIDTH = 128;
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_1 = new java.awt.Color(255, 200, 155);
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_2 = new java.awt.Color(255, 190, 140);

	// Background image drawing facilities
	private BufferedImage canvas;
	private Graphics2D graphics;
	private ArrayList<SuperPath> paths;

	// Animate the background pattern by shifting it horizontally
	private int patternShift = 0;

	private ArrayList<SuperActor> actors;

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
		graphics.setBackground(new java.awt.Color(0, true));

		paths = new ArrayList<SuperPath>();

		actors = new ArrayList<SuperActor>();

		// Draw initial background image so this world isn't blank on reset
		updateBackground();
	}

	/**
	 * Update this world.
	 */
	public void act() {
		updatePaths();
		updateBackground();

		// Add desserts to the last path when mouse is right-clicked
		MouseInfo mouse = Greenfoot.getMouseInfo();
		if (Greenfoot.mousePressed(null) && mouse.getButton() == 3 && paths.size() > 0) {
			actors.add(new Dessert(this, paths.get(paths.size() - 1)));
		}

		// Update and draw actors
		// Use a ListIterator to be able to remove dead actors from the list during iteration
		for (ListIterator<SuperActor> iter = actors.listIterator(); iter.hasNext();) {
			SuperActor actor = iter.next();
			actor.act();
			if (actor.isDead()) {
				iter.remove();
			}
			actor.drawUsingGraphics(graphics);
		}
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

		SuperPath path;
		if (Greenfoot.mousePressed(null) && mouse.getButton() == 1) {
			// When mouse changed from non-pressed to pressed state, begin a new path
			path = new SuperPath();
			paths.add(path);
		} else if (mouse.getButton() == 1 && paths.size() > 0) {
			// Mouse is currently being dragged -> add a new point to the current path
			path = paths.get(paths.size() - 1);
		} else {
			// Not drawing, nothing to do
			return;
		}
		path.addPoint(mouse.getX(), mouse.getY());
	}

	/**
	 * Update this world's background image.
	 */
	private void updateBackground() {
		graphics.clearRect(0, 0, WIDTH, HEIGHT);

		// Draw background pattern
		graphics.setColor(BACKGROUND_PATTERN_COLOR_1);
		graphics.fillRect(0, 0, WIDTH, HEIGHT);
		graphics.setColor(BACKGROUND_PATTERN_COLOR_2);
		for (int x1 = -patternShift; x1 < WIDTH + HEIGHT; x1 += BACKGROUND_PATTERN_WIDTH * 2) {
			int x2 = x1 + BACKGROUND_PATTERN_WIDTH;
			int x3 = x2 - HEIGHT;
			int x4 = x1 - HEIGHT;
			graphics.fillPolygon(new int[] {x1, x2, x3, x4}, new int[] {0, 0, HEIGHT, HEIGHT}, 4);
		}
		// Shift the background pattern for the next act
		patternShift = (patternShift + 1) % (BACKGROUND_PATTERN_WIDTH * 2);

		// Draw paths
		for (SuperPath path : paths) {
			path.drawUsingGraphics(graphics);
		}
	}
}
