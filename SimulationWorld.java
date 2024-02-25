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
	public static final int PATH_OUTLINE_WIDTH = 16;
	public static final java.awt.Color PATH_COLOR = new java.awt.Color(64, 64, 64);

	private static final int BACKGROUND_PATTERN_WIDTH = 128;
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_1 = new java.awt.Color(255, 200, 155);
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_2 = new java.awt.Color(255, 190, 140);

	// Background image drawing facilities
	private BufferedImage canvas;
	private Graphics2D graphics;
	private BasicStroke pathStroke = new BasicStroke(PATH_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private BasicStroke pathOutlineStroke = new BasicStroke(PATH_WIDTH + PATH_OUTLINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
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

		drawPaths();
	}

	private void drawPaths() {
		double[] coords = new double[6];
		for (Path2D.Double path : paths) {
			Path2D.Double fill = null;
			Shape prevSegment = null;
			double prevx = 0;
			double prevy = 0;
			for (PathIterator pi = path.getPathIterator(null); !pi.isDone(); pi.next()) {
				switch (pi.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					prevx = coords[0];
					prevy = coords[1];
					Line2D.Double point = new Line2D.Double(prevx, prevy, prevx, prevy);

					// Draw path outline stroke around this path segment
					graphics.setColor(java.awt.Color.YELLOW);
					graphics.setStroke(pathOutlineStroke);
					graphics.draw(point);

					graphics.setColor(PATH_COLOR);
					graphics.setStroke(pathStroke);
					graphics.draw(point);

					prevSegment = point;
					break;

				case PathIterator.SEG_QUADTO:
					Shape segment;
					if (Math.hypot(coords[2] - prevx, coords[3] - prevy) < PATH_OUTLINE_WIDTH) {
						if (fill == null) {
							fill = new Path2D.Double();
							fill.moveTo(prevx, prevy);
						}
						fill.quadTo(coords[0], coords[1], coords[2], coords[3]);
						segment = fill;
					} else {
						// End the current fill, if any
						if (fill != null) {
							prevSegment = fill;
							fill = null;
						}
						// This individual curve is long enough to treat as a segment
						segment = new QuadCurve2D.Double(prevx, prevy, coords[0], coords[1], coords[2], coords[3]);
					}
					// Draw path outline stroke around this path segment
					graphics.setColor(java.awt.Color.YELLOW);
					graphics.setStroke(pathOutlineStroke);
					graphics.draw(segment);
					// Fill in this path segment
					graphics.setColor(PATH_COLOR);
					graphics.setStroke(pathStroke);
					if (prevSegment != null) {
						// Draw the preceeding segment over the round cap of this segment's outline
						// (hide outline showing in between segments)
						graphics.draw(prevSegment);
					}
					graphics.draw(segment);

					if (fill == null) {
						prevSegment = segment;
					}
					prevx = coords[2];
					prevy = coords[3];
					break;

				default:
					break;
				}
			}
			// This path ends in a filled segment
			if (fill != null) {
				graphics.setColor(java.awt.Color.YELLOW);
				graphics.setStroke(pathOutlineStroke);
				graphics.draw(fill);
				graphics.setColor(PATH_COLOR);
				graphics.setStroke(pathStroke);
				graphics.draw(prevSegment);
				graphics.draw(fill);
			}
		}
	}
}
