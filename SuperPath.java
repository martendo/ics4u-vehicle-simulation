import greenfoot.util.GraphicsUtilities;
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
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * A path that is automatically smoothened with quadratic curves between points
 * and that updates a collection of parallel lanes that follow it while
 * recording all points of self-intersection.
 *
 * A SuperPath maintains two OffsetPathCollection objects: one for the paths of
 * lanes for PathTraveller actors to follow, and one for the path of lane
 * separators for visuals.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class SuperPath {
	// Settings
	public static final boolean SHOW_LANE_PATHS = false;

	// Flatness of curves to enforce when looking for intersections in paths
	private static final double INTERSECTION_TEST_FLATNESS = 1.0;

	// Visual parameters
	public static final int LANE_WIDTH = 50;
	public static final int PATH_OUTLINE_WIDTH = 16;
	private static final java.awt.Color PATH_COLOR = new java.awt.Color(64, 64, 64);
	private static final java.awt.Color PATH_OUTLINE_COLOR = java.awt.Color.YELLOW;
	private static final java.awt.Color LANE_SEPARATOR_COLOR = java.awt.Color.WHITE;
	private static final java.awt.Color LANE_PATH_COLOR = java.awt.Color.RED;
	private static final java.awt.Color KNOT_COLOR = java.awt.Color.PINK;

	private static final BasicStroke LANE_SEPARATOR_STROKE = new BasicStroke(5.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 0.0f, new float[] {15.0f, 30.0f}, 0.0f);
	private static final BasicStroke LANE_PATH_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private static final java.awt.Color HOVER_PATTERN_COLOR_1 = new java.awt.Color(78, 106, 162);
	private static final java.awt.Color HOVER_PATTERN_COLOR_2 = new java.awt.Color(65, 91, 148);
	private static final java.awt.Color SELECTED_PATTERN_COLOR_1 = new java.awt.Color(72, 88, 125);
	private static final java.awt.Color SELECTED_PATTERN_COLOR_2 = new java.awt.Color(64, 79, 116);

	// Paths may be drawn differently depending on their state
	public enum SuperPathState {
		NORMAL, HOVER, SELECTED
	}

	// Special state textures
	private static final BufferedImage HOVER_TEXTURE = createHoverTexture();
	private static final BufferedImage SELECTED_TEXTURE = createSelectedTexture();

	// Anchor rectangles for special state texture paints, shifted as animation
	private static double textureRectX = 0;
	private static Rectangle2D.Double hoverTextureRect = new Rectangle2D.Double(textureRectX, 0, 16, 16);
	private static Rectangle2D.Double selectedTextureRect = new Rectangle2D.Double(textureRectX, 0, 16, 16);

	// Texture paint objects used in place of PATH_COLOR when path is in a special state
	private static TexturePaint hoverPaint = null;
	private static TexturePaint selectedPaint = null;

	// All points in this path, for calculating angles
	private ArrayList<Point2D.Double> points;
	// A single path created from all points added to this SuperPath
	private Path2D.Double path;
	// All points where this SuperPath intersects itself
	private ArrayList<Point2D.Double> intersections;
	// Whether or not this path is being hovered or selected
	private SuperPathState state;
	// Whether or not this path will change
	private boolean isComplete;

	// Paths for individual lanes
	private final int laneCount;
	private final OffsetPathCollection lanes;
	private final OffsetPathCollection laneSeparators;

	// Stored image of this path for more efficient drawing
	private BufferedImage image;
	// Graphics context of this path's image
	private final Graphics2D graphics;
	// Signal that this path has changed and needs to be redrawn
	private boolean needsRedraw;
	// The rectangle that contains this entire path, for cropping its image for faster drawing
	private Rectangle2D bounds;

	// Strokes to draw this path, size based on number of lanes
	private final BasicStroke pathStroke;
	private final BasicStroke pathOutlineStroke;

	// The world this path belongs to, for adding actors that this path creates
	private SimulationWorld world;
	// Objects currently on this path
	private ArrayList<PathTraveller> travellers;
	// Machine actors at ends of this path
	private final Machine startMachine;
	private final Machine endMachine;
	// Spawners for each lane of this path
	private Spawner[] spawners;

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

		// Initialize all data for this path
		points = new ArrayList<Point2D.Double>();
		path = new Path2D.Double();
		intersections = new ArrayList<Point2D.Double>();
		state = SuperPathState.NORMAL;
		isComplete = false;

		// Paths for lanes and lane separators
		this.laneCount = laneCount;
		lanes = new OffsetPathCollection(laneCount, LANE_WIDTH);
		if (laneCount > 1) {
			laneSeparators = new OffsetPathCollection(laneCount - 1, LANE_WIDTH);
		} else {
			laneSeparators = null;
		}

		// Set up visuals
		image = GraphicsUtilities.createCompatibleTranslucentImage(SimulationWorld.WIDTH, SimulationWorld.HEIGHT);
		graphics = image.createGraphics();
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));
		needsRedraw = true;
		bounds = new Rectangle2D.Double(0.0, 0.0, image.getWidth(), image.getHeight());
		// Strokes for drawing this path
		pathStroke = new BasicStroke(getPathWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		pathOutlineStroke = new BasicStroke(getPathWidth() + PATH_OUTLINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		// Initialize path-related actor variables
		world = null;
		travellers = new ArrayList<PathTraveller>();
		startMachine = new Machine(this);
		endMachine = new Machine(this);
		spawners = null;
	}

	/**
	 * Set this path's world to the given world and add its machine actors to the world.
	 *
	 * @param world the SimulationWorld to add this path and its actors to
	 */
	public void addedToWorld(SimulationWorld world) {
		this.world = world;
		world.addActor(startMachine);
		world.addActor(endMachine);
	}

	/**
	 * Get the number of lanes in this SuperPath.
	 */
	public int getLaneCount() {
		return laneCount;
	}

	/**
	 * Get the width of this path, defined by the LANE_WIDTH multiplied by the
	 * number of lanes.
	 */
	public int getPathWidth() {
		return LANE_WIDTH * laneCount;
	}

	/**
	 * Add a new point to this SuperPath.
	 *
	 * @param x the x coordinate of the point
	 * @param y the y coordinate of the point
	 */
	public void addPoint(double x, double y) {
		Point2D.Double prevPoint = (Point2D.Double) path.getCurrentPoint();
		Point2D.Double newPoint = new Point2D.Double();
		if (prevPoint == null) {
			// This is the first point -> begin the path by setting its location
			path.moveTo(x, y);
			// Add a line segment of one point in order to have something to draw
			path.lineTo(x, y);
			newPoint.setLocation(x, y);
			// Update machine locations
			startMachine.setLocation(x, y);
			endMachine.setLocation(x, y);
		} else {
			// Use quadratic curves to smoothen the lines, connecting midpoints
			// of given points with actual points as control points
			double midx = (x + prevx) / 2.0;
			double midy = (y + prevy) / 2.0;
			double ctrlx;
			double ctrly;
			if (prevPoint.x == prevx && prevPoint.y == prevy) {
				// If this curve is a straight line (first curve in path), use
				// the midpoint between start and end points as the control point
				ctrlx = (prevx + midx) / 2.0;
				ctrly = (prevy + midy) / 2.0;
			} else {
				ctrlx = prevx;
				ctrly = prevy;
			}
			QuadCurve2D.Double curve = new QuadCurve2D.Double(prevPoint.x, prevPoint.y, ctrlx, ctrly, midx, midy);
			// Add any new intersection from this curve
			Point2D.Double intersection = getShapeIntersection(path, curve, 1.0, prevPoint);
			if (intersection != null) {
				intersections.add(intersection);
			}
			// Update path
			path.quadTo(curve.ctrlx, curve.ctrly, curve.x2, curve.y2);
			newPoint.setLocation(curve.x2, curve.y2);
			// Update only end machine location
			endMachine.setLocation(curve.x2, curve.y2);

			// Update lanes
			lanes.offsetQuadTo(curve.x1, curve.y1, curve.ctrlx, curve.ctrly, curve.x2, curve.y2);
			if (laneSeparators != null) {
				laneSeparators.offsetQuadTo(curve.x1, curve.y1, curve.ctrlx, curve.ctrly, curve.x2, curve.y2);
			}
		}
		points.add(newPoint);
		startMachine.setRotation(getStartAngle());
		endMachine.setRotation(getEndAngle());

		prevx = x;
		prevy = y;
		needsRedraw = true;
	}

	/**
	 * Draw this SuperPath onto its image object.
	 */
	private void redraw() {
		graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
		// Determine how to paint this path depending on state
		Paint paint;
		if (state == SuperPathState.HOVER) {
			paint = hoverPaint;
		} else if (state == SuperPathState.SELECTED) {
			paint = selectedPaint;
		} else {
			paint = PATH_COLOR;
		}
		// Draw outline of path behind path
		graphics.setColor(PATH_OUTLINE_COLOR);
		graphics.setStroke(pathOutlineStroke);
		strokePath();
		// Draw path surface
		graphics.setPaint(paint);
		graphics.setStroke(pathStroke);
		strokePath();

		// Draw lane separators on top of path
		if (laneSeparators != null) {
			graphics.setColor(LANE_SEPARATOR_COLOR);
			graphics.setStroke(LANE_SEPARATOR_STROKE);
			for (Path2D.Double laneSeparator : laneSeparators.getPaths()) {
				graphics.draw(laneSeparator);
			}
		}

		// Paint over intersections to remove lane separators there (only if there are lane separators)
		if (laneSeparators != null) {
			graphics.setPaint(paint);
			graphics.setStroke(pathStroke);
			for (Point2D.Double p : intersections) {
				graphics.drawLine((int) p.x, (int) p.y, (int) p.x, (int) p.y);
			}
		}

		// Draw lane paths
		if (SHOW_LANE_PATHS) {
			graphics.setColor(LANE_PATH_COLOR);
			graphics.setStroke(LANE_PATH_STROKE);
			for (Path2D.Double lane : lanes.getPaths()) {
				graphics.draw(lane);
			}
		}

		// When in a special state, this path's image changes constantly (animated paint)
		if (state == SuperPathState.NORMAL) {
			needsRedraw = false;
		}
	}

	/**
	 * A helper method to improve the visual quality of SuperPaths.
	 *
	 * Ideally, all calls to this method should be replaceable with
	 * graphics.draw(path), but there are often visual artifacts, particularly
	 * at corners, where line joins are sometimes missing.
	 *
	 * This method separates this path into its component curves and draws them
	 * individually using QuadCurve2D (or Line2D, in the case of the start
	 * point) objects.
	 *
	 * @param graphics the Graphics2D context on which to draw the segment
	 */
	private void strokePath() {
		double[] coords = new double[6];
		double lastx = 0.0;
		double lasty = 0.0;
		for (PathIterator pi = path.getPathIterator(null); !pi.isDone(); pi.next()) {
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
	}

	/**
	 * Return this path's image, redrawing it beforehand if necessary.
	 */
	public BufferedImage getImage() {
		if (needsRedraw) {
			redraw();
		}
		return image;
	}

	/**
	 * Return the X position of the left side of this path's image in its world.
	 */
	public int getX() {
		return (int) bounds.getX();
	}

	/**
	 * Return the Y position of the top side of this path's image in its world.
	 */
	public int getY() {
		return (int) bounds.getY();
	}

	/**
	 * Commit all lane path tails to their respective lane paths for more efficient accessing.
	 */
	public void complete() {
		lanes.complete();
		if (laneSeparators != null) {
			laneSeparators.complete();
		}

		// Crop this path's image to its boundaries for faster drawing
		bounds = pathOutlineStroke.createStrokedShape(path).getBounds2D();
		// Grow bounds to include rendered subpixels
		bounds.setRect(bounds.getX() - 1, bounds.getY() - 1, bounds.getWidth() + 2, bounds.getHeight() + 2);
		// Clamp bounds to original image dimensions
		bounds = bounds.createIntersection(new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()));
		// Draw using cropped image (graphics continues to draw on full-size image, coordinates unaffected)
		image = image.getSubimage(getX(), getY(), (int) bounds.getWidth(), (int) bounds.getHeight());

		// Create dessert spawners for each lane in this path
		spawners = new Spawner[laneCount];
		for (int i = 0; i < laneCount; i++) {
			final int laneNum = i;
			spawners[laneNum] = new Spawner(60, 240) {
				@Override
				public void spawn() {
					Dessert dessert = new Dessert();
					addTraveller(dessert, laneNum);
					world.addActor(dessert);
				}
			};
		}
	}

	/**
	 * Update this path's dessert spawners.
	 */
	public void actSpawners() {
		if (spawners == null) {
			return;
		}
		for (Spawner spawner : spawners) {
			spawner.act();
		}
	}

	/**
	 * Return a list of all actors on this path, including path travellers and machines.
	 */
	public List<SuperActor> getActors() {
		// Append machines so that they are always drawn after (on top of) travellers
		ArrayList<SuperActor> actors = new ArrayList<SuperActor>(travellers);
		actors.add(startMachine);
		actors.add(endMachine);
		return actors;
	}

	/**
	 * Add a path traveller to a lane on this path.
	 *
	 * @param object a path traveller object to travel on this path
	 * @param laneNum the index of the lane to travel on
	 */
	public void addTraveller(PathTraveller object, int laneNum) {
		travellers.add(object);
		object.addedToPath(this, laneNum);
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
	 * Kill all machines and path travellers on this path and clear this path's
	 * traveller list.
	 */
	public void die() {
		startMachine.die();
		endMachine.die();
		// Path travellers will remove themselves from the list when killed
		while (travellers.size() > 0) {
			travellers.get(0).die();
		}
	}

	/**
	 * Get the average angle of the beginning of this path, taking into account
	 * a length defined by a machine's height.
	 */
	public double getStartAngle() {
		ListIterator<Point2D.Double> iter = points.listIterator();
		if (!iter.hasNext()) {
			return 0.0;
		}
		Point2D.Double startPoint = iter.next();
		// Iterate over points until a certain total length between them has been reached
		Point2D.Double prevPoint = startPoint;
		Point2D.Double currentPoint = startPoint;
		for (double dist = 0.0; dist < Machine.WIDTH && iter.hasNext();) {
			currentPoint = iter.next();
			dist += currentPoint.distance(prevPoint);
			prevPoint = currentPoint;
		}
		// Get the angle between the very first point and the first point after a certain length from the start
		return Math.atan2(currentPoint.y - startPoint.y, currentPoint.x - startPoint.x);
	}

	/**
	 * Get the average angle of the end of this path, taking into account a
	 * length defined by a machine's height.
	 */
	public double getEndAngle() {
		ListIterator<Point2D.Double> iter = points.listIterator(points.size());
		if (!iter.hasPrevious()) {
			return 0.0;
		}
		Point2D.Double endPoint = iter.previous();
		// Iterate backwards over points until a certain total length between them has been reached
		Point2D.Double lastPoint = endPoint;
		Point2D.Double currentPoint = endPoint;
		for (double dist = 0.0; dist < Machine.WIDTH && iter.hasPrevious();) {
			currentPoint = iter.previous();
			dist += currentPoint.distance(lastPoint);
			lastPoint = currentPoint;
		}
		// Get the angle between the very last point and the first point after a certain length from the end
		return Math.atan2(endPoint.y - currentPoint.y, endPoint.x - currentPoint.x);
	}

	/**
	 * Get an iterator object that can trace a lane of this SuperPath in
	 * segments of specific lengths.
	 *
	 * @return a new PathTraceIterator that independently traverses the specified lane of this SuperPath
	 */
	public PathTraceIterator getLaneTraceIterator(int laneNum) {
		PathIterator pi = lanes.getPath(laneNum).getPathIterator(null, PathTraceIterator.FLATNESS);
		return new PathTraceIterator(pi);
	}

	/**
	 * Mark this path so it is drawn with the hover pattern.
	 */
	public void markHovered() {
		state = SuperPathState.HOVER;
		needsRedraw = true;
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
		needsRedraw = true;
	}

	/**
	 * Reset this path's state to its default state.
	 */
	public void unsetState() {
		state = SuperPathState.NORMAL;
		needsRedraw = true;
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
	 * Find the first point of intersection between two shapes.
	 *
	 * For simplicity, this method naively iterates over line segments that
	 * approximate the two given shapes, then tests for intersection between
	 * every pair of line segments.
	 *
	 * Note: this method iterates over the second shape during iteration of the
	 * first shape. Thus, it may be more efficient to supply the smaller or less
	 * complex shape as the second.
	 *
	 * @param shapeA the first shape
	 * @param shapeB the second shape
	 * @param distance maximum distance from shapes to allow points of intersection to be found
	 * @param ignorePoint a point that should not be considered an intersection
	 * @return the first point where the two shapes intersect, or null if none is found
	 */
	public static Point2D.Double getShapeIntersection(Shape shapeA, Shape shapeB, double distance, Point2D.Double ignorePoint) {
		Point2D.Double intersection;
		double[] coords = new double[6];
		Line2D.Double lineA = new Line2D.Double();
		Line2D.Double lineB = new Line2D.Double();
		// Iterate over line segments in shapeA
		for (PathIterator iterA = shapeA.getPathIterator(null, INTERSECTION_TEST_FLATNESS); !iterA.isDone(); iterA.next()) {
			switch (iterA.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
				lineA.x2 = coords[0];
				lineA.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lineA.setLine(lineA.x2, lineA.y2, coords[0], coords[1]);
				// Iterate over line segments in shapeB
				for (PathIterator iterB = shapeB.getPathIterator(null, INTERSECTION_TEST_FLATNESS); !iterB.isDone(); iterB.next()) {
					switch (iterB.currentSegment(coords)) {
					case PathIterator.SEG_MOVETO:
						lineB.x2 = coords[0];
						lineB.y2 = coords[1];
						break;
					case PathIterator.SEG_LINETO:
						lineB.setLine(lineB.x2, lineB.y2, coords[0], coords[1]);
						// Test the current pair of line segments
						intersection = getLineIntersection(lineA, lineB, distance);
						if (intersection != null && intersection.distance(ignorePoint) >= 1.0) {
							return intersection;
						}
						break;
					}
				}
				break;
			}
		}
		return null;
	}

	/**
	 * Find the point of intersection between two line segments.
	 *
	 * An intersection that is found to lie just outside the boundaries of a
	 * line segment is still accepted (by the amount specified by distance), in
	 * order to improve visual consistency of paths.
	 *
	 * @param lineA the first line segment
	 * @param lineB the second line segment
	 * @param distance maximum distance from lines to allow points of intersection to be found
	 * @return the point where the two line segments intersect, or null if they do not intersect
	 */
	private static Point2D.Double getLineIntersection(Line2D.Double lineA, Line2D.Double lineB, double distance) {
		double tThreshold = distance / Math.hypot(lineA.x2 - lineA.x1, lineA.y2 - lineA.y1);
		double uThreshold = distance / Math.hypot(lineB.x2 - lineB.x1, lineB.y2 - lineB.y1);
		// See <https://en.wikipedia.org/wiki/Line-line_intersection#Given_two_points_on_each_line_segment>
		// When representing line segments A and B in terms of first degree Bezier parameters,
		//   PA = P1A + t*(P2A - P1A), t in [0, 1]
		//   PB = P1B + u*(P2B - P1B), u in [0, 1]
		// solve for t and u where PA = PB.
		// (The slope-intercept form representation of lines is not sufficient as it cannot represent vertical lines)
		double denominator = (lineA.x1 - lineA.x2) * (lineB.y1 - lineB.y2) - (lineA.y1 - lineA.y2) * (lineB.x1 - lineB.x2);
		double t = ((lineA.x1 - lineB.x1) * (lineB.y1 - lineB.y2) - (lineA.y1 - lineB.y1) * (lineB.x1 - lineB.x2)) / denominator;
		if (t < 0.0 - tThreshold || t > 1.0 + tThreshold) {
			// Point of intersection does not lie within lineA
			return null;
		}
		double u = -((lineA.x1 - lineA.x2) * (lineA.y1 - lineB.y1) - (lineA.y1 - lineA.y2) * (lineA.x1 - lineB.x1)) / denominator;
		if (u < 0.0 - uThreshold || u > 1.0 + uThreshold) {
			// Point of intersection does not lie within lineB
			return null;
		}
		// Substitute t to find coordinates of intersection
		double x = lineA.x1 + t * (lineA.x2 - lineA.x1);
		double y = lineA.y1 + t * (lineA.y2 - lineA.y1);
		return new Point2D.Double(x, y);
	}

	/**
	 * Create and return the image used in the texture to paint hovered paths.
	 */
	private static BufferedImage createHoverTexture() {
		BufferedImage image = GraphicsUtilities.createCompatibleImage(32, 16);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(HOVER_PATTERN_COLOR_1);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		graphics.setColor(HOVER_PATTERN_COLOR_2);
		for (int x1 = 0; x1 < image.getWidth() + image.getHeight(); x1 += 8 * 2) {
			int x2 = x1 + 8;
			int x3 = x2 - image.getHeight();
			int x4 = x1 - image.getHeight();
			graphics.fillPolygon(new int[] {x1, x2, x3, x4}, new int[] {0, 0, image.getHeight(), image.getHeight()}, 4);
		}
		return image;
	}

	/**
	 * Create and return the image used in the texture to paint selected paths.
	 */
	private static BufferedImage createSelectedTexture() {
		BufferedImage image = GraphicsUtilities.createCompatibleImage(32, 16);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(SELECTED_PATTERN_COLOR_1);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		graphics.setColor(SELECTED_PATTERN_COLOR_2);
		for (int x1 = 0; x1 < image.getWidth() + image.getHeight(); x1 += 8 * 2) {
			int x2 = x1 + 8;
			int x3 = x2 - image.getHeight();
			int x4 = x1 - image.getHeight();
			graphics.fillPolygon(new int[] {x1, x2, x3, x4}, new int[] {0, 0, image.getHeight(), image.getHeight()}, 4);
		}
		return image;
	}

	/**
	 * Animate the hovered and selected texture paints. Call this method once per act.
	 */
	public static void updatePaints() {
		// Make texture paints from the current anchor rectangle positions
		hoverPaint = new TexturePaint(HOVER_TEXTURE, hoverTextureRect);
		selectedPaint = new TexturePaint(SELECTED_TEXTURE, selectedTextureRect);
		// Shift the anchor rectangles and wrap around when the ends of the patterns are reached
		textureRectX = (textureRectX + 0.5) % 16.0;
		hoverTextureRect.setRect(textureRectX, 0.0, 16.0, 16.0);
		selectedTextureRect.setRect(textureRectX, 0.0, 16.0, 16.0);
	}
}
