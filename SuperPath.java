import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Shape;
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
 */
public class SuperPath {
	// Visual parameters
	public static final int PATH_WIDTH = 50;
	public static final int PATH_OUTLINE_WIDTH = 16;
	public static final java.awt.Color PATH_COLOR = new java.awt.Color(64, 64, 64);
	public static final java.awt.Color PATH_OUTLINE_COLOR = java.awt.Color.YELLOW;

	// The distance below which to consider points as part of a filled segment
	public static final double FILL_THRESHOLD = PATH_OUTLINE_WIDTH;

	private static final BasicStroke PATH_STROKE = new BasicStroke(PATH_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke PATH_OUTLINE_STROKE = new BasicStroke(PATH_WIDTH + PATH_OUTLINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	// A single path created from all points added to this SuperPath
	private Path2D.Double path;
	// Separated segments for drawing
	private ArrayList<Shape> segments;

	// Variables to keep track of the last given point in order to control path curves
	private double prevx;
	private double prevy;

	/**
	 * Create a new SuperPath.
	 */
	public SuperPath() {
		path = new Path2D.Double();
		segments = new ArrayList<Shape>();
	}

	/**
	 * Add a new point to this SuperPath.
	 *
	 * @param x the x coordinate of the point
	 * @param y the y coordinate of the point
	 */
	public void addPoint(double x, double y) {
		Point2D prevPoint = path.getCurrentPoint();
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
					fillSegment.moveTo(prevPoint.getX(), prevPoint.getY());
					segments.add(fillSegment);
				}
				fillSegment.quadTo(prevx, prevy, midx, midy);
			} else {
				// This individual curve is long enough to treat as a segment on its own
				QuadCurve2D.Double curve = new QuadCurve2D.Double(prevPoint.getX(), prevPoint.getY(), prevx, prevy, midx, midy);
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
		Shape prevSegment = null;
		for (Shape segment : segments) {
			// Draw path outline stroke around this path segment
			graphics.setColor(PATH_OUTLINE_COLOR);
			graphics.setStroke(PATH_OUTLINE_STROKE);
			strokeSegmentUsingGraphics(graphics, segment);

			// Fill in this path segment
			graphics.setColor(PATH_COLOR);
			graphics.setStroke(PATH_STROKE);
			if (prevSegment != null) {
				// Draw the preceeding segment over the round cap of this segment's outline
				// (hide outline showing in between segments)
				strokeSegmentUsingGraphics(graphics, prevSegment);
			}
			strokeSegmentUsingGraphics(graphics, segment);

			prevSegment = segment;
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
	 * Get an iterator object that can trace this SuperPath in segments of specific lengths.
	 *
	 * @return a new PathTraceIterator that independently traverses this SuperPath
	 */
	public PathTraceIterator getPathTraceIterator() {
		PathIterator pi = path.getPathIterator(null, PathTraceIterator.FLATNESS);
		return new PathTraceIterator(pi);
	}
}
