import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

/**
 * A path that is automatically smoothened with quadratic curves between points
 * and divided into segments to create "filled" portions when points are close
 * enough together.
 *
 * The division of SuperPaths into segments allows them to be drawn with a
 * stroked outline that follows the path, even when a SuperPath overlaps itself,
 * where later segments are drawn on top of earlier segments.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class SuperPath {
	// Settings
	public static final boolean SHOW_LANE_PATHS = true;

	// Visual parameters
	public static final int PATH_WIDTH = 50;
	public static final int PATH_OUTLINE_WIDTH = 16;
	public static final java.awt.Color PATH_COLOR = new java.awt.Color(64, 64, 64);
	public static final java.awt.Color PATH_OUTLINE_COLOR = java.awt.Color.YELLOW;
	public static final java.awt.Color LANE_SEPARATOR_COLOR = java.awt.Color.WHITE;
	public static final java.awt.Color LANE_PATH_COLOR = java.awt.Color.RED;
	public static final java.awt.Color KNOT_COLOR = java.awt.Color.PINK;

	private static final BasicStroke LANE_SEPARATOR_STROKE = new BasicStroke(5.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 0.0f, new float[] {15.0f, 30.0f}, 0.0f);
	private static final BasicStroke LANE_PATH_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke KNOT_STROKE = new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	public static final java.awt.Color HOVER_PATTERN_COLOR_1 = new java.awt.Color(78, 106, 162);
	public static final java.awt.Color HOVER_PATTERN_COLOR_2 = new java.awt.Color(65, 91, 148);
	public static final java.awt.Color SELECTED_PATTERN_COLOR_1 = new java.awt.Color(72, 88, 125);
	public static final java.awt.Color SELECTED_PATTERN_COLOR_2 = new java.awt.Color(64, 79, 116);

	// The distance below which to consider points as part of a filled segment
	public static final double FILL_THRESHOLD = PATH_OUTLINE_WIDTH;

	// Paths may be drawn differently depending on their state
	public enum SuperPathState {
		NORMAL, HOVER, SELECTED
	}

	// Special state textures
	private static BufferedImage hoverTexture = null;
	private static BufferedImage selectedTexture = null;
	// Whether or not textures have been created
	private static boolean hasTextures = false;

	// Anchor rectangles for special state texture paints, shifted as animation
	private static double textureRectX = 0.0;
	private static Rectangle2D.Double hoverTextureRect = null;
	private static Rectangle2D.Double selectedTextureRect = null;

	// Texture paint objects used in place of PATH_COLOR when path is in a special state
	private static TexturePaint hoverPaint = null;
	private static TexturePaint selectedPaint = null;

	// Strokes to draw this path, size based on number of lanes
	private BasicStroke pathStroke;
	private BasicStroke pathOutlineStroke;

	// A single path created from all points added to this SuperPath
	private Path2D.Double path;
	// Separated segments for drawing
	private ArrayList<Shape> segments;
	private SuperPathState state;

	// Paths for individual lanes
	private OffsetPathCollection lanes;
	private OffsetPathCollection laneSeparators;

	// Objects currently on this path
	private ArrayList<PathTraveller> travellers;

	// Variables to keep track of the last given point in order to control path curves
	private double prevx;
	private double prevy;

	/**
	 * Create a new SuperPath.
	 *
	 * @param laneCount the number of parallel lanes in this SuperPath
	 */
	public SuperPath(int laneCount) {
		if (laneCount < 1) {
			throw new IllegalArgumentException("Number of lanes must be greater than 0");
		}

		path = new Path2D.Double();
		segments = new ArrayList<Shape>();
		state = SuperPathState.NORMAL;
		travellers = new ArrayList<PathTraveller>();

		// Paths for lanes and lane separators
		lanes = new OffsetPathCollection(laneCount, PATH_WIDTH);
		if (laneCount > 1) {
			laneSeparators = new OffsetPathCollection(laneCount - 1, PATH_WIDTH);
		} else {
			laneSeparators = null;
		}

		// Strokes for drawing this path
		pathStroke = new BasicStroke(PATH_WIDTH * laneCount, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		pathOutlineStroke = new BasicStroke(PATH_WIDTH * laneCount + PATH_OUTLINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}

	/**
	 * Add a new point to this SuperPath.
	 *
	 * @param x the x coordinate of the point
	 * @param y the y coordinate of the point
	 */
	public void addPoint(double x, double y) {
		Point2D.Double prevPoint = (Point2D.Double) path.getCurrentPoint();
		if (prevPoint == null) {
			// This is the first point -> begin the path by setting its location
			path.moveTo(x, y);

			// Add a line segment of one point in order to have something to draw
			Path2D.Double point = new Path2D.Double();
			point.moveTo(x, y);
			point.lineTo(x, y);
			segments.add(point);
		} else {
			// Use quadratic curves to smoothen the lines, connecting midpoints
			// of given points with actual points as control points
			double midx = (x + prevx) / 2.0;
			double midy = (y + prevy) / 2.0;
			path.quadTo(prevx, prevy, midx, midy);
			lanes.offsetQuadTo(prevPoint.x, prevPoint.y, prevx, prevy, midx, midy);
			if (laneSeparators != null) {
				laneSeparators.offsetQuadTo(prevPoint.x, prevPoint.y, prevx, prevy, midx, midy);
			}

			// Update segments
			if (prevPoint.distance(midx, midy) < FILL_THRESHOLD) {
				// This point is close to the previous point, add it to a fill segment
				Path2D.Double fillSegment;
				Shape prevSegment = segments.get(segments.size() - 1);
				if (prevSegment instanceof Path2D.Double) {
					fillSegment = (Path2D.Double) prevSegment;
				} else {
					// Create a new fill segment if the previous segment isn't one
					fillSegment = new Path2D.Double();
					fillSegment.moveTo(prevPoint.x, prevPoint.y);
					segments.add(fillSegment);
				}
				fillSegment.quadTo(prevx, prevy, midx, midy);
			} else {
				// This individual curve is long enough to treat as a segment on its own
				QuadCurve2D.Double curve = new QuadCurve2D.Double(prevPoint.x, prevPoint.y, prevx, prevy, midx, midy);
				segments.add(curve);
			}
		}
		prevx = x;
		prevy = y;
	}

	/**
	 * Draw this SuperPath using a given graphics context.
	 *
	 * @param graphics the Graphics2D context on which to draw this SuperPath
	 */
	public void drawUsingGraphics(Graphics2D graphics) {
		// Determine how to paint this path depending on state
		Paint paint;
		if (state == SuperPathState.HOVER) {
			paint = hoverPaint;
		} else if (state == SuperPathState.SELECTED) {
			paint = selectedPaint;
		} else {
			paint = PATH_COLOR;
		}
		Shape prevSegment = null;
		for (Shape segment : segments) {
			// Draw path outline stroke around this path segment
			graphics.setColor(PATH_OUTLINE_COLOR);
			graphics.setStroke(pathOutlineStroke);
			strokeSegmentUsingGraphics(graphics, segment);

			// Fill in this path segment
			graphics.setPaint(paint);
			graphics.setStroke(pathStroke);
			if (prevSegment != null) {
				// Draw the preceeding segment over the round cap of this segment's outline
				// (hide outline showing in between segments)
				strokeSegmentUsingGraphics(graphics, prevSegment);
			}
			strokeSegmentUsingGraphics(graphics, segment);

			prevSegment = segment;
		}

		// TODO: Draw lanes and lane separators properly
		if (laneSeparators != null) {
			graphics.setStroke(LANE_SEPARATOR_STROKE);
			graphics.setColor(LANE_SEPARATOR_COLOR);
			for (Path2D.Double laneSeparator : laneSeparators.getPaths()) {
				graphics.draw(laneSeparator);
			}
		}

		// Draw lane paths and knot removal points
		if (SHOW_LANE_PATHS) {
			graphics.setStroke(LANE_PATH_STROKE);
			graphics.setColor(LANE_PATH_COLOR);
			for (Path2D.Double lane : lanes.getPaths()) {
				graphics.draw(lane);
			}
			graphics.setStroke(KNOT_STROKE);
			graphics.setColor(KNOT_COLOR);
			for (Point2D.Double p : lanes.getKnots()) {
				graphics.drawLine((int) p.x, (int) p.y, (int) p.x, (int) p.y);
			}
			if (laneSeparators != null) {
				for (Point2D.Double p : laneSeparators.getKnots()) {
					graphics.drawLine((int) p.x, (int) p.y, (int) p.x, (int) p.y);
				}
			}
		}
	}

	/**
	 * A helper method to improve the visual quality of SuperPaths.
	 *
	 * Ideally, all calls to this method should be replaceable with
	 * graphics.draw(segment), but there are often visual artifacts when drawing
	 * "filled" segments (which are objects of the Path2D.Double class),
	 * particularly at corners, where line joins are sometimes missing.
	 *
	 * If the given segment is a "filled" segment, separate it into its
	 * component curves and draw them individually using QuadCurve2D objects.
	 * Otherwise, draw the segment as-is.
	 *
	 * @param graphics the Graphics2D context on which to draw the segment
	 * @param segment the Shape to be drawn
	 */
	private void strokeSegmentUsingGraphics(Graphics2D graphics, Shape segment) {
		if (segment instanceof Path2D.Double) {
			double[] coords = new double[6];
			double lastx = 0.0;
			double lasty = 0.0;
			for (PathIterator pi = segment.getPathIterator(null); !pi.isDone(); pi.next()) {
				switch (pi.currentSegment(coords)) {
				case PathIterator.SEG_MOVETO:
					lastx = coords[0];
					lasty = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					Line2D.Double line = new Line2D.Double(lastx, lasty, coords[0], coords[1]);
					graphics.draw(line);
					lastx = coords[0];
					lasty = coords[1];
					break;
				case PathIterator.SEG_QUADTO:
					QuadCurve2D.Double curve = new QuadCurve2D.Double(lastx, lasty, coords[0], coords[1], coords[2], coords[3]);
					graphics.draw(curve);
					lastx = coords[2];
					lasty = coords[3];
					break;
				default:
					throw new UnsupportedOperationException("Path2D objects within a SuperPath must only consist of segments of type SEG_MOVETO, SEG_LINETO, or SEG_QUADTO");
				}
			}
		} else {
			graphics.draw(segment);
		}
	}

	/**
	 * Add a path traveller to keep track of on this path.
	 *
	 * @param object a path traveller object travelling on this path
	 */
	public void addTraveller(PathTraveller object) {
		travellers.add(object);
	}

	/**
	 * Remove a path traveller from this path.
	 *
	 * @param object the path traveller object to remove from this path
	 */
	public void removeTraveller(PathTraveller object) {
		travellers.remove(object);
	}

	/**
	 * Kill all path travellers on this path and clear this path's traveller list.
	 */
	public void killAllTravellers() {
		// Path travellers will remove themselves from the list when killed
		while (travellers.size() > 0) {
			travellers.get(0).die();
		}
	}

	/**
	 * Get an iterator object that can trace this SuperPath in segments of specific lengths.
	 *
	 * @return a new PathTraceIterator that independently traverses this SuperPath
	 */
	public PathTraceIterator getPathTraceIterator() {
		PathIterator pi = path.getPathIterator(null, PathTraceIterator.FLATNESS);
		return new PathTraceIterator(pi);
	}

	/**
	 * Mark this path so it is drawn with the hover pattern.
	 */
	public void markHovered() {
		state = SuperPathState.HOVER;
	}

	/**
	 * Unmark this path as hovered if it is not currently in another state.
	 */
	public void unmarkHovered() {
		if (state == SuperPathState.HOVER) {
			unsetState();
		}
	}

	/**
	 * Mark this path as selected so it is drawn with the selected pattern.
	 */
	public void select() {
		state = SuperPathState.SELECTED;
	}

	/**
	 * Reset this path's state to its default state.
	 */
	public void unsetState() {
		state = SuperPathState.NORMAL;
	}

	/**
	 * Test if a point is contained within the shape of this path.
	 *
	 * @param x the x-coordinate of the point to test
	 * @param y the y-coordinate of the point to test
	 * @return true if the point lies on top of this path, false otherwise
	 */
	public boolean isPointTouching(double x, double y) {
		// The stroke for the path outline is the thickest, thus defining the boundary of this path
		return pathOutlineStroke.createStrokedShape(path).contains(x, y);
	}

	/**
	 * Create the pattern BufferedImages used to paint hovered and selected paths.
	 */
	private static void createTextures() {
		Graphics2D graphics;
		// Hovered path texture
		hoverTexture = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
		graphics = hoverTexture.createGraphics();
		graphics.setColor(HOVER_PATTERN_COLOR_1);
		graphics.fillRect(0, 0, 32, 16);
		graphics.setColor(HOVER_PATTERN_COLOR_2);
		for (int x1 = 0; x1 < 32 + 16; x1 += 8 * 2) {
			int x2 = x1 + 8;
			int x3 = x2 - 16;
			int x4 = x1 - 16;
			graphics.fillPolygon(new int[] {x1, x2, x3, x4}, new int[] {0, 0, 16, 16}, 4);
		}
		// Selected path texture
		selectedTexture = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
		graphics = selectedTexture.createGraphics();
		graphics.setColor(SELECTED_PATTERN_COLOR_1);
		graphics.fillRect(0, 0, 32, 16);
		graphics.setColor(SELECTED_PATTERN_COLOR_2);
		for (int x1 = 0; x1 < 32 + 16; x1 += 8 * 2) {
			int x2 = x1 + 8;
			int x3 = x2 - 16;
			int x4 = x1 - 16;
			graphics.fillPolygon(new int[] {x1, x2, x3, x4}, new int[] {0, 0, 16, 16}, 4);
		}
	}

	/**
	 * Animate the hovered and selected texture paints. Call this method once per act.
	 */
	public static void updatePaints() {
		if (!hasTextures) {
			// Textures have never been initialized
			createTextures();
			hoverTextureRect = new Rectangle2D.Double(textureRectX, 0.0, 16.0, 16.0);
			selectedTextureRect = new Rectangle2D.Double(textureRectX, 0.0, 16.0, 16.0);
			hasTextures = true;
		}
		// Make texture paints from the current anchor rectangle positions
		hoverPaint = new TexturePaint(hoverTexture, hoverTextureRect);
		selectedPaint = new TexturePaint(selectedTexture, selectedTextureRect);
		// Shift the anchor rectangles and wrap around when the ends of the patterns are reached
		textureRectX = (textureRectX + 0.25) % 16.0;
		hoverTextureRect.setRect(textureRectX, 0.0, 16.0, 16.0);
		selectedTextureRect.setRect(textureRectX, 0.0, 16.0, 16.0);
	}
}
