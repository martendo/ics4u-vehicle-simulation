import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

/**
 * A PathIterator-like class that returns points at specific distances along the
 * path of a Shape.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class PathTraceIterator {
	// Suggested flatness to create PathIterators with for tracing
	// (e.g., used by SuperPath#getPathTraceIterator())
	public static final double FLATNESS = 1.0;

	// Tools to work with the original path
	private final PathIterator originalpi;
	private Point2D lastPathPoint;
	// Current point that is tracing the path
	private Point2D currentPoint;

	/**
	 * Create a new PathTraceIterator using the path defined by the supplied shape.
	 *
	 * @param shape the original path being traced
	 */
	public PathTraceIterator(Shape shape) {
		originalpi = shape.getPathIterator(null, FLATNESS);
		lastPathPoint = null;
		currentPoint = null;
	}

	/**
	 * Get the current path segment in the iteration. The return value is the
	 * path segment type, which will only be either SEG_MOVETO or SEG_LINETO.
	 *
	 * A double array of length 6 must be passed in and is used to store the
	 * coordinates of the point. Each point is stored as a pair of double x,y
	 * coordinates. Since only segments of type SEG_MOVETO or SEG_LINETO are
	 * returned, only one point is ever returned in the coordinate array.
	 *
	 * @param coords an array that holds the data returned from this method
	 * @return the path segment type of the current path segment
	 * @see {@link java.awt.PathIterator#currentSegment(double[])}
	 */
	public int currentSegment(double[] coords) {
		if (lastPathPoint == null) {
			// Begin with a SEG_MOVETO
			return originalpi.currentSegment(coords);
		}
		coords[0] = currentPoint.getX();
		coords[1] = currentPoint.getY();
		return PathIterator.SEG_LINETO;
	}

	/**
	 * Test if the iteration is complete.
	 *
	 * @return true if all segments have been read; false otherwise
	 */
	public boolean isDone() {
		return originalpi.isDone();
	}

	/**
	 * Get the winding rule for determining the interior of the path.
	 *
	 * @return the winding rule
	 * @see java.awt.geom.PathIterator#getWindingRule()
	 */
	public int getWindingRule() {
		return originalpi.getWindingRule();
	}

	/**
	 * Move the iterator the specified distance along the path as long as there
	 * are more points in the forward direction.
	 *
	 * @param distance the distance along the path from the current point at which to find the next point
	 * @throws UnsupportedOperationException if any segment type from the original PathIterator other than SEG_MOVETO and SEG_LINETO is encountered
	 */
	public void next(double distance) {
		double[] coords = new double[6];
		// Loop through segments in case one alone cannot cover the requested distance
		double distRemain = distance;
		while (distRemain > 0.0 && !originalpi.isDone()) {
			switch (originalpi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
				lastPathPoint = new Point2D.Double(coords[0], coords[1]);
				currentPoint = new Point2D.Double(coords[0], coords[1]);
				originalpi.next();
				break;
			case PathIterator.SEG_LINETO:
				double segDist = currentPoint.distance(coords[0], coords[1]);
				if (distRemain < segDist) {
					// Remaining requested distance can be covered within the current segment
					double dx = coords[0] - lastPathPoint.getX();
					double dy = coords[1] - lastPathPoint.getY();
					double angle = Math.atan2(dy, dx);
					currentPoint.setLocation(currentPoint.getX() + distRemain * Math.cos(angle), currentPoint.getY() + distRemain * Math.sin(angle));
					distRemain = 0.0;
				} else {
					// Need to continue to next segment in order to cover requested distance
					currentPoint.setLocation(coords[0], coords[1]);
					distRemain -= segDist;
					originalpi.next();
					lastPathPoint.setLocation(coords[0], coords[1]);
				}
				break;
			}
		}
		// Reached the end of the path -> move to the last point
		if (originalpi.isDone()) {
			currentPoint.setLocation(coords[0], coords[1]);
		}
	}

	/**
	 * Calculate the length of the path that traces the given shape.
	 *
	 * @param shape the path to trace
	 * @return the length of the path defined by shape
	 */
	public static double getLength(Shape shape) {
		double[] coords = new double[6];
		double length = 0.0;
		Point2D lastPoint = new Point2D.Double();
		// Total distances between each returned line segment
		for (PathIterator iter = shape.getPathIterator(null, FLATNESS); !iter.isDone(); iter.next()) {
			switch (iter.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
				lastPoint.setLocation(coords[0], coords[1]);
				break;
			case PathIterator.SEG_LINETO:
				length += lastPoint.distance(coords[0], coords[1]);
				lastPoint.setLocation(coords[0], coords[1]);
				break;
			}
		}
		return length;
	}
}
