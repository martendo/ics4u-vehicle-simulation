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
	public static final boolean DEBUG_SHOW_LANE_PATHS = false;

	// Flatness of curves to enforce when looking for intersections in paths
	private static final double INTERSECTION_TEST_FLATNESS = 1.0;

	// Visual parameters
	public static final int LANE_WIDTH = 50;
	public static final int PATH_OUTLINE_WIDTH = 8;
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
	private static Rectangle2D hoverTextureRect = new Rectangle2D.Double(textureRectX, 0, 16, 16);
	private static Rectangle2D selectedTextureRect = new Rectangle2D.Double(textureRectX, 0, 16, 16);

	// Texture paint objects used in place of PATH_COLOR when path is in a special state
	private static TexturePaint hoverPaint = null;
	private static TexturePaint selectedPaint = null;

	// All points in this path, for calculating angles
	private List<Point2D> points;
	// A single path created from all points added to this SuperPath
	private Path2D path;
	// All points where this SuperPath intersects itself
	private List<Point2D> intersections;
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
	// Objects currently in each lane on this path
	private List<PathTraveller>[] travellers;
	// All spawners attached to this path, stored for cleaning up
	private List<Spawner> spawners;
	// Machine actors at ends of this path
	private final Machine startMachine;
	private final Machine endMachine;

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
		points = new ArrayList<Point2D>();
		path = new Path2D.Double();
		intersections = new ArrayList<Point2D>();
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
		bounds = new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight());
		// Strokes for drawing this path
		pathStroke = new BasicStroke(getPathWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		pathOutlineStroke = new BasicStroke(getPathWidth() + PATH_OUTLINE_WIDTH * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		// Initialize path-related actor variables
		world = null;
		travellers = new List[laneCount];
		for (int i = 0; i < laneCount; i++) {
			travellers[i] = new ArrayList<PathTraveller>();
		}
		spawners = new ArrayList<Spawner>();
		startMachine = new Machine(this, true);
		endMachine = new Machine(this, false);
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
		if (isComplete) {
			throw new UnmodifiablePathException("Cannot modify a completed SuperPath");
		}

		Point2D prevPoint = path.getCurrentPoint();
		Point2D newPoint = new Point2D.Double();
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
			if (prevPoint.getX() == prevx && prevPoint.getY() == prevy) {
				// If this curve is a straight line (first curve in path), use
				// the midpoint between start and end points as the control point
				ctrlx = (prevx + midx) / 2.0;
				ctrly = (prevy + midy) / 2.0;
			} else {
				ctrlx = prevx;
				ctrly = prevy;
			}
			QuadCurve2D curve = new QuadCurve2D.Double(prevPoint.getX(), prevPoint.getY(), ctrlx, ctrly, midx, midy);
			// Add any new intersection from this curve
			Point2D intersection = getShapeIntersection(path, curve, 1.0, prevPoint);
			if (intersection != null) {
				intersections.add(intersection);
			}
			// Update path
			path.quadTo(curve.getCtrlX(), curve.getCtrlY(), curve.getX2(), curve.getY2());
			newPoint.setLocation(curve.getP2());
			// Update only end machine location
			endMachine.setLocation(curve.getX2(), curve.getY2());

			// Update lanes
			lanes.offsetQuadTo(curve.getX1(), curve.getY1(), curve.getCtrlX(), curve.getCtrlY(), curve.getX2(), curve.getY2());
			if (laneSeparators != null) {
				laneSeparators.offsetQuadTo(curve.getX1(), curve.getY1(), curve.getCtrlX(), curve.getCtrlY(), curve.getX2(), curve.getY2());
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
			for (Path2D laneSeparator : laneSeparators.getPaths()) {
				graphics.draw(laneSeparator);
			}
		}

		// Paint over intersections to remove lane separators there (only if there are lane separators)
		if (laneSeparators != null) {
			graphics.setPaint(paint);
			graphics.setStroke(pathStroke);
			for (Point2D p : intersections) {
				int x = (int) p.getX();
				int y = (int) p.getY();
				graphics.drawLine(x, y, x, y);
			}
		}

		// Draw lane paths
		if (DEBUG_SHOW_LANE_PATHS) {
			graphics.setColor(LANE_PATH_COLOR);
			graphics.setStroke(LANE_PATH_STROKE);
			for (Path2D lane : lanes.getPaths()) {
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
				graphics.draw(new Line2D.Double(lastx, lasty, coords[0], coords[1]));
				lastx = coords[0];
				lasty = coords[1];
				break;
			case PathIterator.SEG_QUADTO:
				graphics.draw(new QuadCurve2D.Double(lastx, lasty, coords[0], coords[1], coords[2], coords[3]));
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
		if (isComplete) {
			throw new UnmodifiablePathException("SuperPath is already marked complete");
		}

		isComplete = true;
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
		for (int i = 0; i < laneCount; i++) {
			Spawner dessertSpawner = new DessertSpawner(world, this, i);
			world.addSpawner(dessertSpawner);
			addSpawner(dessertSpawner);
		}
	}

	/**
	 * Return a list of all travellers currently on this path.
	 */
	public List<PathTraveller> getTravellers() {
		List<PathTraveller> result = new ArrayList<PathTraveller>();
		for (List<PathTraveller> laneTravellers : travellers) {
			result.addAll(laneTravellers);
		}
		return result;
	}

	/**
	 * Return a list of all travellers currently in the specified lane on this path.
	 */
	public List<PathTraveller> getTravellersInLane(int laneNum) {
		return new ArrayList<PathTraveller>(travellers[laneNum]);
	}

	/**
	 * Return a list of all actors on this path, including path travellers and machines.
	 */
	public List<SuperActor> getActors() {
		// Append machines so that they are always drawn after (on top of) travellers
		List<SuperActor> actors = new ArrayList<SuperActor>(getTravellers());
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
		travellers[laneNum].add(object);
		object.addedToPath(this, laneNum);
	}

	/**
	 * Remove a path traveller from this path.
	 *
	 * @param object the path traveller object to remove from this path
	 */
	public void removeTraveller(PathTraveller object) {
		travellers[object.getLaneNumber()].remove(object);
	}

	/**
	 * Move a path traveller from its current lane in this path to a new lane.
	 *
	 * @param object the path traveller object to move
	 * @param laneNum the index of the lane to move the traveller to
	 */
	public void moveTravellerToLane(PathTraveller object, int laneNum) {
		if (object.getPath() != this) {
			throw new IllegalArgumentException("PathTraveller object does not belong to this SuperPath");
		}
		travellers[object.getLaneNumber()].remove(object);
		travellers[laneNum].add(object);
	}

	/**
	 * Add a spawner to this path's spawner list so it may be removed from its
	 * world when this path dies.
	 *
	 * @param spawner the spawner to add to this path's spawner list
	 */
	public void addSpawner(Spawner spawner) {
		spawners.add(spawner);
	}

	/**
	 * Remove a spawner from this path's spawner list.
	 *
	 * @param spawner the spawner to remove from this path's spawner list
	 */
	public void removeSpawner(Spawner spawner) {
		spawners.remove(spawner);
	}

	/**
	 * Kill all machines and path travellers on this path and remove all of its
	 * spawners from the world.
	 */
	public void die() {
		startMachine.die();
		endMachine.die();
		// Path travellers will remove themselves from the list when killed
		for (List<PathTraveller> laneTravellers : travellers) {
			while (laneTravellers.size() > 0) {
				laneTravellers.get(0).die();
			}
		}
		// Remove spawners
		for (Spawner spawner : spawners) {
			world.removeSpawner(spawner);
		}
		spawners.clear();
	}

	/**
	 * Get the average angle of the beginning of this path, taking into account
	 * a length defined by a machine's height.
	 */
	public double getStartAngle() {
		ListIterator<Point2D> iter = points.listIterator();
		if (!iter.hasNext()) {
			return 0.0;
		}
		Point2D startPoint = iter.next();
		// Iterate over points until a certain total length between them has been reached
		Point2D prevPoint = startPoint;
		Point2D currentPoint = startPoint;
		for (double dist = 0.0; dist < 32.0 && iter.hasNext();) {
			currentPoint = iter.next();
			dist += currentPoint.distance(prevPoint);
			prevPoint = currentPoint;
		}
		// Get the angle between the very first point and the first point after a certain length from the start
		return Math.atan2(currentPoint.getY() - startPoint.getY(), currentPoint.getX() - startPoint.getX());
	}

	/**
	 * Get the average angle of the end of this path, taking into account a
	 * length defined by a machine's height.
	 */
	public double getEndAngle() {
		ListIterator<Point2D> iter = points.listIterator(points.size());
		if (!iter.hasPrevious()) {
			return 0.0;
		}
		Point2D endPoint = iter.previous();
		// Iterate backwards over points until a certain total length between them has been reached
		Point2D lastPoint = endPoint;
		Point2D currentPoint = endPoint;
		for (double dist = 0.0; dist < 32.0 && iter.hasPrevious();) {
			currentPoint = iter.previous();
			dist += currentPoint.distance(lastPoint);
			lastPoint = currentPoint;
		}
		// Get the angle between the very last point and the first point after a certain length from the end
		return Math.atan2(endPoint.getY() - currentPoint.getY(), endPoint.getX() - currentPoint.getX());
	}

	/**
	 * Get an iterator object that can trace a lane of this SuperPath in
	 * segments of specific lengths.
	 *
	 * @return a new PathTraceIterator that independently traverses the specified lane of this SuperPath
	 */
	public PathTraceIterator getLaneTraceIterator(int laneNum) {
		return new PathTraceIterator(lanes.getPath(laneNum));
	}

	/**
	 * Get the total length of the path of the lane at the specified index.
	 *
	 * @param laneNum the index of the lane to check
	 * @return the length of the lane
	 */
	public double getLaneLength(int laneNum) {
		return PathTraceIterator.getLength(lanes.getPath(laneNum));
	}

	/**
	 * Get the distance along a lane in this path for a point that is adjacent
	 * to the point at a given distance along another lane.
	 *
	 * @param srcLane the index of the lane where the distance is given
	 * @param srcDist the distance along the source lane to get the equivalent distance to
	 * @param destLane the index of the lane to find the distance in
	 * @return the distance for a point on the target lane that is adjacent to the point at the given distance on the source lane
	 */
	public double getAdjacentDistanceInLane(int srcLane, double srcDist, int destLane) {
		double[] coords = new double[6];
		// Get the angle of the normal to the path at the point found at the given distance
		PathTraceIterator srcIter = getLaneTraceIterator(srcLane);
		srcIter.next(srcDist);
		srcIter.currentSegment(coords);
		double srcX = coords[0];
		double srcY = coords[1];
		// Simply use a point 1 pixel further along to calculate the angle
		srcIter.next(1.0);
		srcIter.currentSegment(coords);
		double normal = Math.atan2(coords[1] - srcY, coords[0] - srcX) + Math.PI / 2.0;
		// Get the target point, the ideal location of the point in the adjacent lane
		// Since lanes are modified in order to improve their usability (e.g. splicing knots out),
		// this exact point may not exist in the lane
		double destX = srcX + LANE_WIDTH * (destLane - srcLane) * Math.cos(normal);
		double destY = srcY + LANE_WIDTH * (destLane - srcLane) * Math.sin(normal);

		// Find the point in the lane that is closest to the target point
		// NOTE: It may under very rare circumstances be possible to find an
		// incorrect point that is closer to the target point than the real goal
		Point2D closestPoint = new Point2D.Double();
		double closestPointDist = -1.0;
		double destDist = 0.0;
		double traversed = 0.0;
		for (PathTraceIterator destIter = getLaneTraceIterator(destLane); !destIter.isDone(); destIter.next(1.0)) {
			destIter.currentSegment(coords);
			double pointDist = Math.hypot(destX - coords[0], destY - coords[1]);
			if (pointDist < closestPointDist || closestPointDist < 0.0) {
				closestPoint.setLocation(coords[0], coords[1]);
				closestPointDist = pointDist;
				destDist = traversed;
			}
			traversed += 1.0;
		}
		return destDist;
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
	public static Point2D getShapeIntersection(Shape shapeA, Shape shapeB, double distance, Point2D ignorePoint) {
		Point2D intersection;
		double[] coords = new double[6];
		Line2D lineA = new Line2D.Double();
		Line2D lineB = new Line2D.Double();
		// Iterate over line segments in shapeA
		for (PathIterator iterA = shapeA.getPathIterator(null, INTERSECTION_TEST_FLATNESS); !iterA.isDone(); iterA.next()) {
			switch (iterA.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
				lineA.setLine(0, 0, coords[0], coords[1]);
				break;
			case PathIterator.SEG_LINETO:
				lineA.setLine(lineA.getX2(), lineA.getY2(), coords[0], coords[1]);
				// Iterate over line segments in shapeB
				for (PathIterator iterB = shapeB.getPathIterator(null, INTERSECTION_TEST_FLATNESS); !iterB.isDone(); iterB.next()) {
					switch (iterB.currentSegment(coords)) {
					case PathIterator.SEG_MOVETO:
						lineB.setLine(0, 0, coords[0], coords[1]);
						break;
					case PathIterator.SEG_LINETO:
						lineB.setLine(lineB.getX2(), lineB.getY2(), coords[0], coords[1]);
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
	private static Point2D getLineIntersection(Line2D lineA, Line2D lineB, double distance) {
		double tThreshold = distance / Math.hypot(lineA.getX2() - lineA.getX1(), lineA.getY2() - lineA.getY1());
		double uThreshold = distance / Math.hypot(lineB.getX2() - lineB.getX1(), lineB.getY2() - lineB.getY1());
		// See <https://en.wikipedia.org/wiki/Line-line_intersection#Given_two_points_on_each_line_segment>
		// When representing line segments A and B in terms of first degree Bezier parameters,
		//   PA = P1A + t*(P2A - P1A), t in [0, 1]
		//   PB = P1B + u*(P2B - P1B), u in [0, 1]
		// solve for t and u where PA = PB.
		// (The slope-intercept form representation of lines is not sufficient as it cannot represent vertical lines)
		double denominator = (lineA.getX1() - lineA.getX2()) * (lineB.getY1() - lineB.getY2()) - (lineA.getY1() - lineA.getY2()) * (lineB.getX1() - lineB.getX2());
		double t = ((lineA.getX1() - lineB.getX1()) * (lineB.getY1() - lineB.getY2()) - (lineA.getY1() - lineB.getY1()) * (lineB.getX1() - lineB.getX2())) / denominator;
		if (t < 0.0 - tThreshold || t > 1.0 + tThreshold) {
			// Point of intersection does not lie within lineA
			return null;
		}
		double u = -((lineA.getX1() - lineA.getX2()) * (lineA.getY1() - lineB.getY1()) - (lineA.getY1() - lineA.getY2()) * (lineA.getX1() - lineB.getX1())) / denominator;
		if (u < 0.0 - uThreshold || u > 1.0 + uThreshold) {
			// Point of intersection does not lie within lineB
			return null;
		}
		// Substitute t to find coordinates of intersection
		double x = lineA.getX1() + t * (lineA.getX2() - lineA.getX1());
		double y = lineA.getY1() + t * (lineA.getY2() - lineA.getY1());
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
