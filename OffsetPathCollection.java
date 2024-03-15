import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.Deque;
import java.util.ArrayDeque;

/**
 * A class to provide tools for updating a collection of parallel paths
 * consisting of quadratic curves. Paths in the collection are automatically
 * adjusted based on their offset distances when adding new curves.
 *
 * When the curvature of the original curves is sharp, the offsetted paths
 * naturally contain knots. This class will automatically find these knots and
 * splice them out of any paths where they appear, by joining the two ends of
 * the path at the point of self-intersection.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class OffsetPathCollection {
	// Distance from the end of each path to look for knots
	public static final double KNOT_TEST_DISTANCE = SuperPath.LANE_WIDTH * Math.PI * 2.0;

	// Number of paths in this collection
	private final int size;
	// Path objects
	private final Path2D[] paths;
	// Offset of each path in this collection
	private final double[] offsets;
	// Queue of curves at the current end of each path for knot testing, uncommitted to the Path2D objects until they are far enough away
	private final Deque<QuadCurve2D>[] pathTails;
	// Current (approximate) length of the paths stored in pathTails
	private final double[] pathTailLengths;

	// Whether or not the Path2D objects in paths can be considered to be complete paths (path tail is empty)
	private boolean isComplete;

	/**
	 * Create a new collection of parallel paths spaced at equal distances.
	 *
	 * @param size the number of paths in this collection
	 * @param offset the distance between each path in this collection
	 */
	public OffsetPathCollection(int size, double offset) {
		if (size < 1) {
			throw new IllegalArgumentException("Number of paths in collection must be greater than 0");
		}

		this.size = size;
		paths = new Path2D[size];
		offsets = new double[size];
		pathTails = new Deque[size];
		pathTailLengths = new double[size];
		for (int i = 0; i < size; i++) {
			paths[i] = new Path2D.Double();
			offsets[i] = (double) offset * ((double) i - (double) (size - 1) / 2.0);
			pathTails[i] = new ArrayDeque<QuadCurve2D>();
			pathTailLengths[i] = 0.0;
		}
		isComplete = false;
	}

	/**
	 * Return an array containing the paths in this collection, including tail queues.
	 */
	public Path2D[] getPaths() {
		if (isComplete) {
			return paths;
		}
		// Need to append uncomitted path tails
		Path2D[] result = new Path2D.Double[size];
		for (int i = 0; i < size; i++) {
			result[i] = getPath(i);
		}
		return result;
	}

	/**
	 * Return the path in this collection at the specified index, including its tail queue.
	 */
	public Path2D getPath(int index) {
		if (isComplete) {
			return paths[index];
		}
		// Need to append uncomitted path tail
		Path2D path = new Path2D.Double(paths[index]);
		for (QuadCurve2D curve : pathTails[index]) {
			path.quadTo(curve.getCtrlX(), curve.getCtrlY(), curve.getX2(), curve.getY2());
		}
		return path;
	}

	/**
	 * Commit all path tails to their respective paths for more efficient accessing.
	 */
	public void complete() {
		for (int i = 0; i < size; i++) {
			while (!pathTails[i].isEmpty()) {
				QuadCurve2D curve = pathTails[i].removeFirst();
				paths[i].quadTo(curve.getCtrlX(), curve.getCtrlY(), curve.getX2(), curve.getY2());
			}
		}
		isComplete = true;
	}

	/**
	 * Add a quadratic curve to all paths in this collection. An offset curve
	 * for each path is calculated using the given points, making all paths
	 * follow the specified curve parallel to each other.
	 *
	 * The true offset curve of a Bezier curve cannot be represented exactly by
	 * another Bezier curve, so the paths created by this method are not
	 * perfect. However, it aims to be "close enough".
	 *
	 * For simplicity, the offset curve is created by offsetting each leg of the
	 * control polygon defining the original curve by the offset amount in the
	 * direction perpendicular to each leg. This is "close enough".
	 *
	 * @param x1 the x-coordinate of the start point
	 * @param y1 the y-coordinate of the start point
	 * @param ctrlx the x-coordinate of the quadratic control point
	 * @param ctrly the y-coordinate of the quadratic control point
	 * @param x2 the x-coordinate of the end point
	 * @param y2 the y-coordinate of the end point
	 */
	public void offsetQuadTo(double x1, double y1, double ctrlx, double ctrly, double x2, double y2) {
		isComplete = false;
		double startNormal = Math.atan2(ctrly - y1, ctrlx - x1) + Math.PI / 2.0;
		double endNormal = Math.atan2(y2 - ctrly, x2 - ctrlx) + Math.PI / 2.0;
		double ctrlAngle = Math.atan2(y2 - y1, x2 - x1) + Math.PI / 2.0;
		for (int i = 0; i < size; i++) {
			// Offset start point by the normal to the first edge of the control polygon
			double x1Offset = offsets[i] * Math.cos(startNormal);
			double y1Offset = offsets[i] * Math.sin(startNormal);
			if (paths[i].getCurrentPoint() == null && pathTails[i].isEmpty()) {
				paths[i].moveTo(x1 + x1Offset, y1 + y1Offset);
			}
			// Offset control point by the normal to the baseline of the control polygon
			double ctrlxOffset = offsets[i] * Math.cos(ctrlAngle);
			double ctrlyOffset = offsets[i] * Math.sin(ctrlAngle);
			// Offset end point by the normal to the second edge of the control polygon
			double x2Offset = offsets[i] * Math.cos(endNormal);
			double y2Offset = offsets[i] * Math.sin(endNormal);

			QuadCurve2D curve = new QuadCurve2D.Double(x1 + x1Offset, y1 + y1Offset, ctrlx + ctrlxOffset, ctrly + ctrlyOffset, x2 + x2Offset, y2 + y2Offset);
			updatePathTail(i, curve);

			if (pathTailLengths[i] > KNOT_TEST_DISTANCE) {
				QuadCurve2D tail = pathTails[i].removeFirst();
				paths[i].quadTo(tail.getCtrlX(), tail.getCtrlY(), tail.getX2(), tail.getY2());
				pathTailLengths[i] -= Math.hypot(tail.getX2() - tail.getX1(), tail.getY2() - tail.getY1());
			}
		}
	}

	/**
	 * Find an intersection between the tail of the path at the given index and
	 * the given curve, then trim the end of the path to the found point of
	 * intersection.
	 *
	 * This is necessary to remove "knots" from offset curves, which may contain
	 * cusps at sharp corners of the original path.
	 *
	 * @param index the index of the path in this collection to trim
	 * @param curve the curve to test for intersection with
	 */
	private void updatePathTail(int index, QuadCurve2D curve) {
		Point2D knot = null;
		QuadCurve2D trim = null;
		for (QuadCurve2D pathCurve : pathTails[index]) {
			knot = SuperPath.getShapeIntersection(pathCurve, curve, 5.0, curve.getP1());
			if (knot != null) {
				// The curve intersects with this part of the path, prepare to trim it
				trim = pathCurve;
				break;
			}
		}
		if (knot == null) {
			// No intersection was found: the curve can be added to the tail of this path directly
			pathTails[index].addLast(curve);
			pathTailLengths[index] += Math.hypot(curve.getX2() - curve.getX1(), curve.getY2() - curve.getY1());
			return;
		}

		// End the intersecting part of the path at the point of intersection
		pathTailLengths[index] -= Math.hypot(trim.getX2() - trim.getX1(), trim.getY2() - trim.getY1());
		trim.setCurve(trim.getX1(), trim.getY1(), (trim.getX1() + knot.getX()) / 2.0, (trim.getY1() + knot.getY()) / 2.0, knot.getX(), knot.getY());
		pathTailLengths[index] += Math.hypot(trim.getX2() - trim.getX1(), trim.getY2() - trim.getY1());
		// Remove the remainder of the path that follows this part
		while (pathTails[index].peekLast() != trim) {
			QuadCurve2D last = pathTails[index].removeLast();
			pathTailLengths[index] -= Math.hypot(last.getX2() - last.getX1(), last.getY2() - last.getY1());
		}
		// Add the given curve to this path, starting at the point of intersection
		pathTails[index].addLast(new QuadCurve2D.Double(knot.getX(), knot.getY(), (knot.getX() + curve.getX2()) / 2.0, (knot.getY() + curve.getY2()) / 2.0, curve.getX2(), curve.getY2()));
		pathTailLengths[index] += Math.hypot(curve.getX2() - knot.getX(), curve.getY2() - knot.getY());
	}
}
