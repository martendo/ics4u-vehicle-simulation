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
	private SelectButton[] buttons;
	private ArrayList<SelectButton> shownButtons;
	private static final int BUTTON_INDEX_DRAW = 0;
	private static final int BUTTON_INDEX_SELECT = 1;
	private static final int BUTTON_INDEX_DELETE = 2;

	// Background image drawing facilities
	private BufferedImage canvas;
	private Graphics2D graphics;
	private ArrayList<SuperPath> paths;
	private PathEditMode pathEditMode;
	private boolean isDrawing;
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
		buttons = new SelectButton[3];
		buttons[BUTTON_INDEX_DRAW] = new SelectButton(new GreenfootImage("images/pencil.png"), new Callback() {
			public void run() {
				pathEditMode = PathEditMode.DRAW;
				buttons[0].select();
				for (int i = 1; i < buttons.length; i++) {
					buttons[i].deselect();
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
				// Hide path delete button since there are no longer any selected paths
				hideButton(buttons[BUTTON_INDEX_DELETE]);
			}
		}, true);
		buttons[BUTTON_INDEX_SELECT] = new SelectButton(new GreenfootImage("images/select.png"), new Callback() {
			public void run() {
				pathEditMode = PathEditMode.SELECT;
				buttons[1].select();
				for (int i = 0; i < buttons.length; i++) {
					if (i != 1) {
						buttons[i].deselect();
					}
				}
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
				hideButton(buttons[BUTTON_INDEX_DELETE]);
			}
		}, false);
		shownButtons = new ArrayList<SelectButton>();
		shownButtons.add(buttons[BUTTON_INDEX_DRAW]);
		shownButtons.add(buttons[BUTTON_INDEX_SELECT]);
		displayButtons();

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
			actors.add(new Dessert(paths.get(paths.size() - 1)));
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
		switch (pathEditMode) {
		case DRAW:
			if (Greenfoot.mousePressed(this) && mouse.getButton() == 1) {
				// When mouse changed from non-pressed to pressed state, begin a new path
				path = new SuperPath();
				paths.add(path);
				isDrawing = true;
			} else if (isDrawing) {
				// Stop drawing when mouse is released, but still add the release point to the current path
				if (Greenfoot.mouseClicked(null)) {
					isDrawing = false;
				}
				path = paths.get(paths.size() - 1);
			} else {
				// Not drawing, nothing to do
				return;
			}
			path.addPoint(mouse.getX(), mouse.getY());
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
					showButton(buttons[BUTTON_INDEX_DELETE]);
				} else {
					hideButton(buttons[BUTTON_INDEX_DELETE]);
				}
			} else if (Greenfoot.mouseMoved(this)) {
				// Clear hover state on any previously hovered path
				if (hoveredPath != null) {
					hoveredPath.unmarkHovered();
					hoveredPath = null;
				}
				// Iterate backwards through paths because later paths appear on top
				for (ListIterator<SuperPath> iter = paths.listIterator(paths.size()); iter.hasPrevious();) {
					path = iter.previous();
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

	private void showButton(SelectButton button) {
		if (shownButtons.contains(button)) {
			return;
		}
		shownButtons.add(button);
		displayButtons();
	}

	private void hideButton(SelectButton button) {
		removeObjects(shownButtons);
		shownButtons.remove(button);
		displayButtons();
	}

	private void displayButtons() {
		for (int i = 0; i < shownButtons.size(); i++) {
			addObject(shownButtons.get(i), 20 * (i + 1) + (SelectButton.WIDTH * i) + SelectButton.WIDTH / 2, HEIGHT - 20 - SelectButton.HEIGHT / 2);
		}
	}
}
