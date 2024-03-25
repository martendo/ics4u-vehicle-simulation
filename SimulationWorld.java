import greenfoot.*;
import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Composite;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * ICS4U Vehicle Simulation
 * Martin Baldwin
 *
 * Welcome to a jumbled up and twisted land where birds and aliens don't quite
 * get along, and maybe we should introduce some city planning regulations.
 *
 * CONTENTS
 *
 * (i)   DESCRIPTION OF INTERACTIONS
 * (ii)  OVERVIEW OF SPECIAL FEATURES
 * (iii) LIST OF SIMULATION ELEMENTS
 * (iv)  CREDITS
 * (v)   KNOWN BUGS
 * (vi)  EXTRA INFORMATION
 *
 *
 *
 * (i) DESCRIPTION OF INTERACTIONS
 *
 * - Vehicles drive along paths, and most of them carry items. When a vehicle
 *   gets stuck behind another slower vehicle, it will attempt to change to an
 *   adjacent lane.
 *   - Vehicles can crash into each other (try drawing a looped road!)
 *   - Vehicles can be blown up by Explosions
 *   - Vehicles speed up during an Alien Invasion
 *   - Vehicles can be abducted by UFOs during an Alien Invasion
 *   - Rarity from most common to most rare: Candy, Poison = Bomb, Zapper
 * - Birds fly around the world (at a constant speed) while targeting the
 *   closest food item (Candy or Poison) carried by specific types of vehicles.
 *   - When a Bird eats a Candy, it speeds up and flies in a happy random path (image change)
 *   - When a Bird eats a Poison, it slows down and flies in a miserable random path (image change)
 *   - Birds can be blown up by Explosions
 *   - Birds are scared away during an Alien Invasion
 * - UFOs fly from one edge of the world to the opposite edge in a straight line
 *   while moving in a spiral-like motion. They abduct any items and any Birds
 *   directly underneath them.
 *   - UFOs are scared away by Explosions (image change)
 *   - UFOs additionally abduct vehicles during an Alien Invasion (image change)
 *   - UFOS spawn more frequently during an Alien Invasion
 * - Zappers are a rare and slow type of vehicle that will periodically zap the
 *   closest UFO, destroying it.
 *
 *
 *
 * (ii) OVERVIEW OF SPECIAL FEATURES
 *
 * 1. Free-hand vehicle path drawing
 *
 * Create custom paths for vehicles to follow by drawing freely with the mouse.
 * The number of parallel lanes to draw in each path can be set using the plus
 * and minus buttons at the bottom of the world.
 *
 * Roads are created by fitting quadratic curves between points recorded from
 * the mouse, which are then offset a certain distance to create parallel curves
 * for lanes.
 *
 * 2. Z layers
 *
 * All actors exist on a layer that is tied to the paths that exist in the
 * world. Examples of how this comes into play: "Wanderers" (Pedestrians) can
 * only interact with actors on a layer on or below their own (as both types are
 * flying objects). Zappers can only zap UFOs when there is no path between it
 * and its target UFO on the Z axis. Vehicles do not interact with vehicles on
 * other layers, etc.
 *
 * 3. Graphics
 *
 * Since Greenfoot's built-in graphics manipulation capabilities are
 * insufficient for this program, I almost exclusively use Java AWT's Graphics2D
 * to create the visuals in the world. This gives me the ability to make use of
 * dashed curves (lane separator lines) or thick-stroked lines (roads), for
 * example.
 *
 *
 *
 * (iii) LIST OF SIMULATION ELEMENTS
 *
 * so it's clear how I mean to fulfill the requirements
 *
 * - Vehicles:
 *   1. Candy, feeds Birds
 *   2. Poison, sickens Birds
 *   3. Bomb, explodes after some time
 *   4. Zapper, kills UFOs
 *
 * Lane change decision-making code present in Truck class
 *
 * - Pedestrians:
 *   1. Bird, flies while targeting food (Candy and Poison), can get fed or sick
 *   2. UFO, flies across world, abducting any items or Birds (or vehicles) underneath
 *
 * - Local effect:
 *      bomb explodes, killing vehicles and birds in range
 * - Worldwide effect:
 *      alien invasion (see (i) for description)
 *
 * - Sound effects:
 *   1. Vehicle honking horn when changing lanes (Vehicle)
 *   2. Zapper zapping UFO (Vehicle)
 *   3. Vehicle to vehicle collision (Vehicle)
 *   4. Bird getting fed (Pedestrian)
 *   5. Bird getting sick (Pedestrian)
 *   6. UFO abducting (Pedestrian)
 *   7. UFO getting scared (Pedestrian)
 *   8. Alien invasion (Worldwide effect)
 *   9. Bomb explosion (Local effect)
 *  10. Background ambience
 *  11. Path edit button click (UI)
 *
 * A note on the background sounds: it's supposed to be various vehicle noises
 * plus bird noises. Like all of the sounds, it is Nintendo Game Boy 8-bit audio.
 *
 *
 *
 * (iv) CREDITS
 *
 * Formula for calculating the intersection of two line segments in
 * SuperPath.getLineIntersection() adapted from Wikipedia:
 *     <https://en.wikipedia.org/wiki/Line-line_intersection#Given_two_points_on_each_line_segment>
 *
 * Everything else original by Martin Baldwin
 *
 * All images created with GIMP:
 *     <https://gimp.org>
 *
 * All sounds created with Game Boy Tracker by Stephane Hockenhull:
 *     <https://www.vgmpf.com/Wiki/index.php/Game_Boy_Tracker>
 *
 * All sounds recorded from bgb emulator by Bas Steendijk:
 *     <https://bgb.bircd.org>
 *
 * Sound editing done in Audacity:
 *     <https://audacityteam.org>
 *
 *
 *
 * (v) KNOWN BUGS
 *
 * Not a bug, but...
 *
 * For smoother (less "glitchy"-looking) roads, it is recommended that you
 * reduce the act speed when drawing. Paths can become extremely messy when they
 * are creating lanes that are offset between points that are very close
 * together, which leads to vehicles following the spiky and weird paths that
 * this creates. Basically, it's your fault for drawing ugly roads. (Though with
 * a lot of work, the program could be made to fix it for you... that's too
 * complicated for me.)
 *
 * Try setting SuperPath.DEBUG_SHOW_LANE_PATHS to true to have a clearer look.
 *
 * Also not a bug, but...
 *
 * Self-intersecting roads are hard to deal with. The OffsetPathCollection class
 * automatically removes "knots" from paths in order to have lanes that make
 * sense (see (vi), #2 Knots), but there are times where it's hard to judge when
 * to do so, leading to having only some lanes "taking a shortcut" across loops
 * but not others.
 *
 * Try adjusting OffsetPathCollection.KNOT_TEST_DISTANCE to see the difference.
 *
 *
 *
 * (vi) EXTRA INFORMATION
 *
 * Noteworthy obstacles (some fairly technical details)
 *
 * 1. Parallel curves
 *
 * Every path is made up of a series of quadratic Bezier curves, but the true
 * offset curve of a Bezier curve cannot be represented exactly by another
 * Bezier curve. So, the offset curves are actually just approximate curves made
 * by offsetting the control polygon's legs and creating a curve using the
 * offsetted control polygon.
 *
 * Relevant code: OffsetPathCollection.offsetQuadTo()
 *
 * 2. Knots
 *
 * Since a curve parallel to another curve may naturally contain cusps and
 * overlapping sections where the curvature of the original curve is sharp (see
 * <https://commons.wikimedia.org/wiki/File:Offset-curves-of-sinus-curve.svg>
 * for an example), it is necessary to remove these sections from paths. All
 * lane paths are tested for self-intersection and any "knots" that are found
 * are spliced from the path.
 *
 * Relevant code: OffsetPathCollection.updatePathTail(), SuperPath.getShapeIntersection()
 *
 * 3. Lane changing, hard mode
 *
 * Because of that splicing, lanes are not the same length. So, in order to
 * figure out where to position a vehicle when changing lanes, I have to find
 * the point that is offset by the lane width in the direction of the normal to
 * the path at the current point and trace the target lane to find where that
 * point can be reached. This could theoretically fail in extremely rare cases,
 * but it appears to work well enough.
 *
 * Relevant code: Truck.canMoveToLane(), SuperPath.getAdjacentDistanceInLane()
 *
 * 4. Rendering
 *
 * Normally, Greenfoot will paint the world on its own, separately from running
 * the simulation. However, since I need to control the graphics myself, I'm
 * doing nearly all of the painting during the act cycle. This can be quite
 * slow (if you bring the speed slider up all the way, it might actually not
 * speed up at all).
 *
 * One technique that I employ in order to help mitigate this issue slightly is
 * cropping images, particularly those of paths (but also Tunnels).
 *
 * Relevant code: Try setting SuperPath.DEBUG_SHOW_BOUNDING_RECT and
 * Tunnel.DEBUG_SHOW_BOUNDING_RECT to true
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class SimulationWorld extends World {
	// Settings
	public static final int MIN_LANE_COUNT = 1;
	public static final int MAX_LANE_COUNT = 10;
	public static final boolean INIT_DEFAULT_PATH = true;

	// Dimensions of this world
	public static final int WIDTH = 1024;
	public static final int HEIGHT = 768;

	// Map of rendering hints to be used in all graphics contexts
	public static final RenderingHints renderingHints;

	static {
		// Turning on antialiasing gives smoother-looking graphics
		renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// Allow drawing images with subpixel accuracy (for precise positioning and rotation)
		renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		// Set all other applicable hints to prefer speed
		renderingHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		renderingHints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
		renderingHints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
	}

	private static final int DEFAULT_DRAW_LANE_COUNT = 2;

	// Background pattern visual parameters
	private static final int BACKGROUND_PATTERN_WIDTH = 128;
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_1 = new java.awt.Color(94, 175, 86);
	private static final java.awt.Color BACKGROUND_PATTERN_COLOR_2 = new java.awt.Color(87, 165, 80);
	private static final BufferedImage BACKGROUND_PATTERN = createBackgroundPattern();

	private static final GreenfootSound BACKGROUND_SOUND = new GreenfootSound("sounds/background.wav");

	// The order to draw actor types that are not linked to paths, appearing from bottom to top
	// Any actors that are not linked to a path (instanceof PathTraveller or Tunnel) and that are
	// not an instance of a class included here are drawn on top of the world, above everything else
	private static final Class[] NONPATH_ACTOR_DRAW_ORDER = {Explosion.class, Bird.class, Ufo.class, Zap.class};

	// Mouse actions can correspond to different path-editing actions depending on the selected button
	public enum PathEditMode {
		DRAW, SELECT
	}

	// The list of current widgets being shown to the user
	private List<Widget> shownWidgets;
	// All buttons for this world
	private final SelectButton[] buttons;
	// Constants for accessing buttons in the buttons array
	private static int BUTTON_COUNT = 0;
	private static final int BUTTON_INDEX_DRAW = BUTTON_COUNT++;
	private static final int BUTTON_INDEX_SELECT = BUTTON_COUNT++;
	private static final int BUTTON_INDEX_DELETE = BUTTON_COUNT++;
	private static final int BUTTON_INDEX_LANE_MINUS = BUTTON_COUNT++;
	private static final int BUTTON_INDEX_LANE_PLUS = BUTTON_COUNT++;
	// All other widgets for this world
	private final Widget[] otherWidgets;
	// Constants for accessing other widgets in the widgets array
	private static int OTHER_WIDGET_COUNT = 0;
	private static final int WIDGET_INDEX_LANE_COUNT = OTHER_WIDGET_COUNT++;

	// Background image drawing facilities
	private final BufferedImage canvas;
	private final Graphics2D graphics;
	private List<SuperPath> paths;
	private PathEditMode pathEditMode;

	// Path drawing state
	private boolean isDrawing;
	private int drawLaneCount;
	// Path selecting state
	private SuperPath hoveredPath;
	private SuperPath selectedPath;

	// Animate the background pattern by shifting it horizontally
	private int patternShift;

	// All non-Greenfoot actors in this world
	private List<SuperActor> actors;
	// All spawner objects in this world
	private List<Spawner> spawners;

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
		graphics.addRenderingHints(renderingHints);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));

		// Initialize path editing variables
		paths = new ArrayList<SuperPath>();
		pathEditMode = PathEditMode.DRAW;
		isDrawing = false;
		hoveredPath = null;
		selectedPath = null;

		patternShift = 0;
		actors = new ArrayList<SuperActor>();
		spawners = new ArrayList<Spawner>();

		/*
		 * Create the data structures holding this world's widgets, including
		 * buttons and other widgets, create the widgets themselves, and set up the
		 * default widget configuration.
		 */
		// Set up path-editing buttons
		buttons = new SelectButton[BUTTON_COUNT];
		// Draw button that switches to path drawing mode when clicked
		buttons[BUTTON_INDEX_DRAW] = new SelectButton(new GreenfootImage("images/pencil.png"), true) {
			@Override
			public void clicked() {
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
				// Show lane count widgets
				showWidget(buttons[BUTTON_INDEX_LANE_MINUS]);
				showWidget(otherWidgets[WIDGET_INDEX_LANE_COUNT]);
				showWidget(buttons[BUTTON_INDEX_LANE_PLUS]);
				// Hide path delete button since there are no longer any selected paths
				hideWidget(buttons[BUTTON_INDEX_DELETE]);
			}
		};
		// Select button that switches to path selection mode when clicked
		buttons[BUTTON_INDEX_SELECT] = new SelectButton(new GreenfootImage("images/select.png"), false) {
			@Override
			public void clicked() {
				pathEditMode = PathEditMode.SELECT;
				buttons[BUTTON_INDEX_SELECT].select();
				for (int i = 0; i < buttons.length; i++) {
					if (i != BUTTON_INDEX_SELECT) {
						buttons[i].deselect();
					}
				}
				// Hide lane count widgets
				hideWidget(buttons[BUTTON_INDEX_LANE_MINUS]);
				hideWidget(otherWidgets[WIDGET_INDEX_LANE_COUNT]);
				hideWidget(buttons[BUTTON_INDEX_LANE_PLUS]);
			}
		};
		// Delete button that deletes the currently selected path, if any
		buttons[BUTTON_INDEX_DELETE] = new SelectButton(new GreenfootImage("images/trash.png"), false) {
			@Override
			public void clicked() {
				if (selectedPath == null) {
					return;
				}
				selectedPath.die();
				removeActorLayer(paths.indexOf(selectedPath));
				paths.remove(selectedPath);
				selectedPath = null;
				hideWidget(buttons[BUTTON_INDEX_DELETE]);
			}
		};
		// Minus button that decrements the current lane drawing count
		buttons[BUTTON_INDEX_LANE_MINUS] = new SelectButton(new GreenfootImage("images/minus.png"), false) {
			@Override
			public void clicked() {
				setDrawLaneCount(drawLaneCount - 1);
			}
		};
		// Plus button that increments the current lane drawing count
		buttons[BUTTON_INDEX_LANE_PLUS] = new SelectButton(new GreenfootImage("images/plus.png"), false) {
			@Override
			public void clicked() {
				setDrawLaneCount(drawLaneCount + 1);
			}
		};

		// Set up other widgets
		otherWidgets = new Widget[OTHER_WIDGET_COUNT];
		// Lane drawing count that displays the number of lanes to draw new paths with
		otherWidgets[WIDGET_INDEX_LANE_COUNT] = new Widget(null);

		// Display initial widgets
		shownWidgets = new ArrayList<Widget>();
		shownWidgets.add(buttons[BUTTON_INDEX_DRAW]);
		shownWidgets.add(buttons[BUTTON_INDEX_SELECT]);
		shownWidgets.add(buttons[BUTTON_INDEX_LANE_MINUS]);
		shownWidgets.add(otherWidgets[WIDGET_INDEX_LANE_COUNT]);
		shownWidgets.add(buttons[BUTTON_INDEX_LANE_PLUS]);
		displayWidgets();

		setDrawLaneCount(DEFAULT_DRAW_LANE_COUNT);

		if (INIT_DEFAULT_PATH) {
			// Create one default path to start with
			SuperPath defaultPath = new SuperPath(3);
			paths.add(defaultPath);
			defaultPath.addedToWorld(this);
			defaultPath.addPoint(925, 25);
			defaultPath.addPoint(875, 225);
			defaultPath.addPoint(750, 275);
			defaultPath.addPoint(575, 175);
			defaultPath.addPoint(350, 125);
			defaultPath.addPoint(200, 150);
			defaultPath.addPoint(100, 275);
			defaultPath.addPoint(125, 450);
			defaultPath.addPoint(300, 550);
			defaultPath.addPoint(550, 400);
			defaultPath.addPoint(700, 450);
			defaultPath.addPoint(725, 650);
			defaultPath.addPoint(750, 775);
			defaultPath.complete();
		}

		/*
		 * Create and add to the world the spawners that it uses throughout the
		 * entire simulation.
		 */
		// Create wanderer spawners
		spawners.add(new RandomSpawner(120, 300) {
			@Override
			public void run() {
				spawnUfo();
			}
		});
		spawners.add(new RandomSpawner(60, 180) {
			@Override
			public void run() {
				if (AlienInvasion.isActive()) {
					return;
				}
				Bird bird = new Bird();
				if (!paths.isEmpty()) {
					bird.setLayer((int) (Math.random() * paths.size()));
				}
				addActor(bird);
			}
		});
		// Create worldwide effect spawner
		spawners.add(new RandomSpawner(1800, 2400) {
			@Override
			public void run() {
				addActor(new AlienInvasion());
			}
		});

		// Reset invasion effect state
		AlienInvasion.init();

		// Draw initial background image so this world isn't blank on reset
		updateImage();
	}

	/**
	 * Create and return the image to be used as a world's background pattern.
	 */
	private static BufferedImage createBackgroundPattern() {
		BufferedImage image = GraphicsUtilities.createCompatibleImage(WIDTH + BACKGROUND_PATTERN_WIDTH * 2, HEIGHT);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(BACKGROUND_PATTERN_COLOR_1);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		graphics.setColor(BACKGROUND_PATTERN_COLOR_2);
		for (int x1 = 0; x1 < image.getWidth() + HEIGHT; x1 += BACKGROUND_PATTERN_WIDTH * 2) {
			int x2 = x1 + BACKGROUND_PATTERN_WIDTH;
			int x3 = x2 - HEIGHT;
			int x4 = x1 - HEIGHT;
			graphics.fillPolygon(new int[] {x1, x2, x3, x4}, new int[] {0, 0, HEIGHT, HEIGHT}, 4);
		}
		graphics.dispose();
		return image;
	}

	/**
	 * Add a SuperActor to this world.
	 *
	 * @param actor the SuperActor object to add
	 */
	public void addActor(SuperActor actor) {
		actors.add(actor);
		actor.setWorld(this);
	}

	/**
	 * Get the index (layer index) of the given path in this world.
	 *
	 * @param path the SuperPath to test
	 */
	public int getPathIndex(SuperPath path) {
		return paths.indexOf(path);
	}

	/**
	 * Shift all actors on or above the given layer down to the layer below,
	 * effectively removing the given layer.
	 *
	 * @param layer the index of the layer to remove
	 */
	public void removeActorLayer(int layer) {
		for (SuperActor actor : actors) {
			if (actor.getLayer() >= layer) {
				actor.setLayer(Math.max(actor.getLayer() - 1, -1));
			}
		}
	}

	/**
	 * Add a spawner to this world.
	 *
	 * @param spawner the spawner object to add
	 */
	public void addSpawner(Spawner spawner) {
		spawners.add(spawner);
	}

	/**
	 * Remove a spawner from this world.
	 *
	 * @param spawner the spawner object to add
	 */
	public void removeSpawner(Spawner spawner) {
		spawners.remove(spawner);
	}

	/**
	 * Create and add a new UFO actor to this world.
	 */
	public void spawnUfo() {
		Ufo ufo = new Ufo();
		if (!paths.isEmpty()) {
			ufo.setLayer((int) (Math.random() * paths.size()));
		}
		addActor(ufo);
	}

	/**
	 * Update this world.
	 */
	@Override
	public void act() {
		updatePathEditing();

		// Update spawners
		// Create a copy of the spawners list to allow adding and removing spawners during iteration
		for (Spawner spawner : new ArrayList<Spawner>(spawners)) {
			if (spawner.isDone()) {
				spawners.remove(spawner);
				continue;
			}
			spawner.act();
			if (spawner.isDone()) {
				spawners.remove(spawner);
			}
		}

		// Update actors
		// Create a copy of the actors list to allow adding and removing spawners during iteration
		for (SuperActor actor : new ArrayList<SuperActor>(actors)) {
			if (actor.isDead()) {
				actors.remove(actor);
				continue;
			}
			actor.act();
		}
		// Remove newly dead actors
		for (ListIterator<SuperActor> iter = actors.listIterator(); iter.hasNext();) {
			if (iter.next().isDead()) {
				iter.remove();
			}
		}

		// Render
		updateImage();
	}

	/**
	 * Pause any background sounds and any currently playing sound effects.
	 */
	@Override
	public void stopped() {
		BACKGROUND_SOUND.pause();
		AlienInvasion.pauseSound();
		SoundEffect.pauseAllSounds();
	}

	/**
	 * Resume any background sounds and any previously paused sound effects.
	 */
	@Override
	public void started() {
		BACKGROUND_SOUND.playLoop();
		AlienInvasion.resumeSound();
		SoundEffect.resumeAllSounds();
	}

	/**
	 * Update the paths in this world based on mouse events, allowing the user to draw.
	 */
	private void updatePathEditing() {
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
				path.addedToWorld(this);
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
				SuperPath underPath = getPathUnderPoint(mouse.getX(), mouse.getY());
				// Don't bother updating the hover state if the path is already selected
				if (underPath != null && underPath != selectedPath) {
					underPath.markHovered();
					hoveredPath = underPath;
				}
			}
			break;
		}
	}

	/**
	 * Update this world's background image.
	 */
	private void updateImage() {
		graphics.drawImage(BACKGROUND_PATTERN, -patternShift, 0, null);
		// Shift the background pattern for the next act
		patternShift = (patternShift + 1) % (BACKGROUND_PATTERN_WIDTH * 2);

		// Keep track of which actors to draw, as actors on paths are given special priority
		List<SuperActor> actorsToDraw = new ArrayList<SuperActor>(actors);

		if (hoveredPath != null || selectedPath != null) {
			SuperPath.updatePaints();
		}
		// Draw paths
		for (int layer = -1; layer < paths.size(); layer++) {
			if (layer >= 0) {
				SuperPath path = paths.get(layer);
				graphics.drawImage(path.getImage(), path.getX(), path.getY(), null);
				for (SuperActor actor : path.getActors()) {
					drawActor(actor);
					// This actor no longer needs to be drawn
					actorsToDraw.remove(actor);
				}
			}
			// Draw non-path actors on this layer
			for (Class cls : NONPATH_ACTOR_DRAW_ORDER) {
				for (ListIterator<SuperActor> iter = actorsToDraw.listIterator(); iter.hasNext();) {
					SuperActor actor = iter.next();
					if (cls.isInstance(actor) && actor.getLayer() == layer) {
						drawActor(actor);
						iter.remove();
					}
				}
			}
		}
		// Draw remaining actors on top of the world
		for (SuperActor actor : actorsToDraw) {
			drawActor(actor);
		}
	}

	/**
	 * Draw a SuperActor's image onto this world's background image at the
	 * actor's image location.
	 */
	private void drawActor(SuperActor actor) {
		graphics.setComposite(actor.getImageComposite());
		graphics.drawImage(actor.getImage(), actor.getImageTransform(), null);
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
		Widget widget = otherWidgets[WIDGET_INDEX_LANE_COUNT];
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

	/**
	 * Get all actors in this world of the specified class.
	 *
	 * @param cls the class of the actors to retrieve
	 * @return a list of all actors of the class cls in this world
	 */
	public <T extends SuperActor> List<T> getActors(Class<T> cls) {
		List<T> result = new ArrayList<T>();
		for (SuperActor actor : actors) {
			if (cls.isInstance(actor)) {
				result.add(cls.cast(actor));
			}
		}
		return result;
	}

	/**
	 * Get all actors in this world of the specified class on or below the given
	 * layer.
	 *
	 * @param cls the class of the actors to retrieve
	 * @param layer the index of the topmost layer to retrieve actors from
	 * @return a list of all actors of the class on or below the given layer in this world
	 */
	public <T extends SuperActor> List<T> getActors(Class<T> cls, int layer) {
		List<T> result = new ArrayList<T>();
		for (SuperActor actor : actors) {
			if (cls.isInstance(actor) && actor.getLayer() <= layer) {
				result.add(cls.cast(actor));
			}
		}
		return result;
	}

	/**
	 * Get the topmost path in this world that touches the given point on or
	 * below the given layer.
	 *
	 * @param x the x-coordinate of the point to test
	 * @param y the y-coordinate of the point to test
	 * @param fromLayer the index of the topmost path to test
	 * @return the visually-topmost SuperPath object under the point on or below the given layer, or null if there is no path there
	 */
	public SuperPath getPathUnderPoint(double x, double y, int fromLayer) {
		// Iterate backwards through paths because later paths appear on top
		for (ListIterator<SuperPath> iter = paths.listIterator(fromLayer + 1); iter.hasPrevious();) {
			SuperPath path = iter.previous();
			if (path.isPointTouching(x, y)) {
				return path;
			}
		}
		return null;
	}

	/**
	 * Get the topmost path in this world that touches the given point.
	 *
	 * @param x the x-coordinate of the point to test
	 * @param y the y-coordinate of the point to test
	 * @return the visually-topmost SuperPath object under the point, or null if there is no path there
	 */
	public SuperPath getPathUnderPoint(double x, double y) {
		return getPathUnderPoint(x, y, paths.size() - 1);
	}
}
