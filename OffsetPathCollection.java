import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
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
	public static final double KNOT_TEST_DISTANCE = SuperPath.PATH_WIDTH * Math.PI * 2.0;

	// Number of paths in this collection
	private int size;
	// Path objects
	private Path2D.Double[] paths;
	// Offset of each path in this collection
	private double[] offsets;
	// Queue of curves at the current end of each path, uncommitted to the Path2D objects until they are far enough away in order to test for knots
	private ArrayDeque<QuadCurve2D.Double>[] pathTails;
	// Current (approximate) length of the paths stored in pathTails
	private double[] pathTailLengths;

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
		paths = new Path2D.Double[size];
		offsets = new double[size];
		pathTails = new ArrayDeque[size];
		pathTailLengths = new double[size];
		for (int i = 0; i < size; i++) {
			paths[i] = new Path2D.Double();
			offsets[i] = (double) offset * ((double) i - (double) (size - 1) / 2.0);
			pathTails[i] = new ArrayDeque<QuadCurve2D.Double>();
			pathTailLengths[i] = 0.0;
		}
		isComplete = false;
	}

	/**
	 * Return an array containing the paths in this collection, including tail queues.
	 */
	public Path2D.Double[] getPaths() {
		if (isComplete) {
			return paths;
		}
		// Need to append uncomitted path tails
		Path2D.Double[] result = new Path2D.Double[size];
		for (int i = 0; i < size; i++) {
			result[i] = getPath(i);
		}
		return result;
	}

	/**
	 * Return the path in this collection at the specified index, including its tail queue.
	 */
	public Path2D.Double getPath(int index) {
		if (isComplete) {
			return paths[index];
		}
		// Need to append uncomitted path tail
		Path2D.Double path = (Path2D.Double) paths[index].clone();
		for (QuadCurve2D.Double curve : pathTails[index]) {
			path.quadTo(curve.ctrlx, curve.ctrly, curve.x2, curve.y2);
		}
		return path;
	}

	/**
	 * Commit all path tails to their respective paths for more efficient accessing.
	 */
	public void complete() {
		for (int i = 0; i < size; i++) {
			while (!pathTails[i].isEmpty()) {
				QuadCurve2D.Double curve = pathTails[i].removeFirst();
				paths[i].quadTo(curve.ctrlx, curve.ctrly, curve.x2, curve.y2);
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

			QuadCurve2D.Double curve = new QuadCurve2D.Double(x1 + x1Offset, y1 + y1Offset, ctrlx + ctrlxOffset, ctrly + ctrlyOffset, x2 + x2Offset, y2 + y2Offset);
			updatePathTail(i, curve);

			if (pathTailLengths[i] > KNOT_TEST_DISTANCE) {
				QuadCurve2D.Double lastCurve = pathTails[i].removeFirst();
				paths[i].quadTo(lastCurve.ctrlx, lastCurve.ctrly, lastCurve.x2, lastCurve.y2);
				pathTailLengths[i] -= Math.hypot(lastCurve.x2 - lastCurve.x1, lastCurve.y2 - lastCurve.y1);
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
	private void updatePathTail(int index, QuadCurve2D.Double curve) {
		Point2D.Double intersection = null;
		QuadCurve2D.Double trimCurve = null;
		for (QuadCurve2D.Double pathCurve : pathTails[index]) {
			intersection = SuperPath.getShapeIntersection(pathCurve, curve, 5.0, new Point2D.Double(curve.x1, curve.y1));
			if (intersection != null) {
				// The curve intersects with this part of the path, prepare to trim it
				trimCurve = pathCurve;
				break;
			}
		}
		if (intersection == null) {
			// No intersection was found: the curve can be added to the tail of this path directly
			pathTails[index].addLast(curve);
			pathTailLengths[index] += Math.hypot(curve.x2 - curve.x1, curve.y2 - curve.y1);
			return;
		}

		// End the intersecting part of the path at the point of intersection
		pathTailLengths[index] -= Math.hypot(trimCurve.x2 - trimCurve.x1, trimCurve.y2 - trimCurve.y1);
		trimCurve.setCurve(trimCurve.x1, trimCurve.y1, (trimCurve.x1 + intersection.x) / 2.0, (trimCurve.y1 + intersection.y) / 2.0, intersection.x, intersection.y);
		pathTailLengths[index] += Math.hypot(trimCurve.x2 - trimCurve.x1, trimCurve.y2 - trimCurve.y1);
		// Remove the remainder of the path that follows this part
		while (pathTails[index].peekLast() != trimCurve) {
			QuadCurve2D.Double lastCurve = pathTails[index].removeLast();
			pathTailLengths[index] -= Math.hypot(lastCurve.x2 - lastCurve.x1, lastCurve.y2 - lastCurve.y1);
		}
		// Add the given curve to this path, starting at the point of intersection
		pathTails[index].addLast(new QuadCurve2D.Double(intersection.x, intersection.y, (intersection.x + curve.x2) / 2.0, (intersection.y + curve.y2) / 2.0, curve.x2, curve.y2));
		pathTailLengths[index] += Math.hypot(curve.x2 - intersection.x, curve.y2 - intersection.y);
	}
}
