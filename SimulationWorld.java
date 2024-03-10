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
 * @version March 2024
 */
public class SimulationWorld extends World {
	// Settings
	public static final int MIN_LANE_COUNT = 1;
	public static final int MAX_LANE_COUNT = 10;

	// Dimensions of this world
	public static final int WIDTH = 1024;
	public static final int HEIGHT = 768;

	private static final int BACKGROUND_PATTERN_WIDTH = 128;
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_1 = new java.awt.Color(255, 200, 155);
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_2 = new java.awt.Color(255, 190, 140);

	// Mouse actions can correspond to different path-editing actions depending on the selected button
	public enum PathEditMode {
		DRAW, SELECT
	}
	private ArrayList<Widget> shownWidgets;
	private SelectButton[] buttons;
	private static int BUTTON_COUNT = 0;
	private static final int BUTTON_INDEX_DRAW = BUTTON_COUNT++;
	private static final int BUTTON_INDEX_SELECT = BUTTON_COUNT++;
	private static final int BUTTON_INDEX_DELETE = BUTTON_COUNT++;
	private static final int BUTTON_INDEX_LANE_MINUS = BUTTON_COUNT++;
	private static final int BUTTON_INDEX_LANE_PLUS = BUTTON_COUNT++;
	private Widget[] widgets;
	private static int WIDGET_COUNT = 0;
	private static final int WIDGET_INDEX_LANE_COUNT = WIDGET_COUNT++;

	// Background image drawing facilities
	private BufferedImage canvas;
	private Graphics2D graphics;
	private ArrayList<SuperPath> paths;
	private PathEditMode pathEditMode;

	// Path drawing state
	private boolean isDrawing;
	private int drawLaneCount;
	// Path selecting state
	private SuperPath hoveredPath;
	private SuperPath selectedPath;

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
		pathEditMode = PathEditMode.DRAW;
		isDrawing = false;
		hoveredPath = null;
		selectedPath = null;

		actors = new ArrayList<SuperActor>();

		// Set up path-editing buttons
		buttons = new SelectButton[BUTTON_COUNT];
		buttons[BUTTON_INDEX_DRAW] = new SelectButton(new GreenfootImage("images/pencil.png"), new Callback() {
			public void run() {
				pathEditMode = PathEditMode.DRAW;
				buttons[BUTTON_INDEX_DRAW].select();
				for (int i = 0; i < buttons.length; i++) {
					if (i != BUTTON_INDEX_DRAW) {
						buttons[i].deselect();
					}
				}
				// Deselect any currently hovered/selected paths
				if (hoveredPath != null) {
					hoveredPath.unsetState();
					hoveredPath = null;
				}
				if (selectedPath != null) {
					selectedPath.unsetState();
					selectedPath = null;
				}
				showWidget(buttons[BUTTON_INDEX_LANE_MINUS]);
				showWidget(widgets[WIDGET_INDEX_LANE_COUNT]);
				showWidget(buttons[BUTTON_INDEX_LANE_PLUS]);
				// Hide path delete button since there are no longer any selected paths
				hideWidget(buttons[BUTTON_INDEX_DELETE]);
			}
		}, true);
		buttons[BUTTON_INDEX_SELECT] = new SelectButton(new GreenfootImage("images/select.png"), new Callback() {
			public void run() {
				pathEditMode = PathEditMode.SELECT;
				buttons[BUTTON_INDEX_SELECT].select();
				for (int i = 0; i < buttons.length; i++) {
					if (i != BUTTON_INDEX_SELECT) {
						buttons[i].deselect();
					}
				}
				hideWidget(buttons[BUTTON_INDEX_LANE_MINUS]);
				hideWidget(widgets[WIDGET_INDEX_LANE_COUNT]);
				hideWidget(buttons[BUTTON_INDEX_LANE_PLUS]);
			}
		}, false);
		buttons[BUTTON_INDEX_DELETE] = new SelectButton(new GreenfootImage("images/trash.png"), new Callback() {
			public void run() {
				if (selectedPath == null) {
					return;
				}
				selectedPath.killAllTravellers();
				selectedPath.unsetState();
				paths.remove(selectedPath);
				selectedPath = null;
				hideWidget(buttons[BUTTON_INDEX_DELETE]);
			}
		}, false);
		buttons[BUTTON_INDEX_LANE_MINUS] = new SelectButton(new GreenfootImage("images/minus.png"), new Callback() {
			public void run() {
				setDrawLaneCount(drawLaneCount - 1);
			}
		}, false);
		buttons[BUTTON_INDEX_LANE_PLUS] = new SelectButton(new GreenfootImage("images/plus.png"), new Callback() {
			public void run() {
				setDrawLaneCount(drawLaneCount + 1);
			}
		}, false);

		// Set up other widgets
		widgets = new Widget[WIDGET_COUNT];
		widgets[WIDGET_INDEX_LANE_COUNT] = new Widget(null);
		setDrawLaneCount(2);

		// Display initial widgets
		shownWidgets = new ArrayList<Widget>();
		shownWidgets.add(buttons[BUTTON_INDEX_DRAW]);
		shownWidgets.add(buttons[BUTTON_INDEX_SELECT]);
		shownWidgets.add(buttons[BUTTON_INDEX_LANE_MINUS]);
		shownWidgets.add(widgets[WIDGET_INDEX_LANE_COUNT]);
		shownWidgets.add(buttons[BUTTON_INDEX_LANE_PLUS]);
		displayWidgets();

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
		if (Greenfoot.mousePressed(this) && mouse.getButton() == 3 && paths.size() > 0) {
			SuperPath path = paths.get(paths.size() - 1);
			for (int i = 0; i < path.getLaneCount(); i++) {
				actors.add(new Dessert(path, i));
			}
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

		switch (pathEditMode) {
		case DRAW:
			if (Greenfoot.mousePressed(this) && mouse.getButton() == 1) {
				// When mouse changed from non-pressed to pressed state, begin a new path
				SuperPath path = new SuperPath(drawLaneCount);
				path.addPoint(mouse.getX(), mouse.getY());
				paths.add(path);
				isDrawing = true;
			} else if (isDrawing) {
				SuperPath path = paths.get(paths.size() - 1);
				// Stop drawing when mouse is released, but still add the release point to the current path
				if (Greenfoot.mouseClicked(null)) {
					isDrawing = false;
					path.addPoint(mouse.getX(), mouse.getY());
					path.complete();
				} else if (Greenfoot.mouseDragged(null)) {
					path.addPoint(mouse.getX(), mouse.getY());
				}
			}
			break;
		case SELECT:
			if (Greenfoot.mousePressed(this)) {
				// Deselect any previously selected path
				if (selectedPath != null) {
					selectedPath.unsetState();
				}
				// Select the currently hovered path, if any
				selectedPath = hoveredPath;
				if (selectedPath != null) {
					selectedPath.select();
					hoveredPath = null;
					showWidget(buttons[BUTTON_INDEX_DELETE]);
				} else {
					hideWidget(buttons[BUTTON_INDEX_DELETE]);
				}
			} else if (Greenfoot.mouseMoved(null)) {
				// Clear hover state on any previously hovered path
				if (hoveredPath != null) {
					hoveredPath.unmarkHovered();
					hoveredPath = null;
				}
				// Iterate backwards through paths because later paths appear on top
				for (ListIterator<SuperPath> iter = paths.listIterator(paths.size()); iter.hasPrevious();) {
					SuperPath path = iter.previous();
					// Don't bother updating the hover state if the path is already selected
					if (path == selectedPath) {
						continue;
					}
					if (path.isPointTouching(mouse.getX(), mouse.getY())) {
						path.markHovered();
						hoveredPath = path;
						break;
					}
				}
			}
			break;
		}
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
		SuperPath.updatePaints();
		for (SuperPath path : paths) {
			path.drawUsingGraphics(graphics);
		}
	}

	/**
	 * Set the number of lanes to draw new paths with, and update the widget
	 * that displays this number. The value is clamped to be at least
	 * MIN_LANE_COUNT and at most MAX_LANE_COUNT.
	 *
	 * @param count the value to request to set the current lane count for drawing
	 */
	private void setDrawLaneCount(int count) {
		drawLaneCount = Math.max(MIN_LANE_COUNT, Math.min(MAX_LANE_COUNT, count));
		Widget widget = widgets[WIDGET_INDEX_LANE_COUNT];
		widget.setIcon(new GreenfootImage(String.valueOf(drawLaneCount), 48, Color.BLACK, Color.WHITE));
	}

	/**
	 * Add a widget to be shown to the user. It will appear in the rightmost position.
	 *
	 * @param widget the widget to show
	 */
	private void showWidget(Widget widget) {
		if (shownWidgets.contains(widget)) {
			return;
		}
		shownWidgets.add(widget);
		displayWidgets();
	}

	/**
	 * Remove a widget from the list of widgets being shown to the user.
	 *
	 * @param widget the widget to hide
	 */
	private void hideWidget(Widget widget) {
		// Order of widgets will change, remove them before adding again
		removeObjects(shownWidgets);
		shownWidgets.remove(widget);
		displayWidgets();
	}

	/**
	 * Add all widgets in the list of widgets to be shown to the user to this world.
	 */
	private void displayWidgets() {
		for (int i = 0; i < shownWidgets.size(); i++) {
			addObject(shownWidgets.get(i), 20 * (i + 1) + (Widget.WIDTH * i) + Widget.WIDTH / 2, HEIGHT - 20 - Widget.HEIGHT / 2);
		}
	}
}
