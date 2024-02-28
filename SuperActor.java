import java.awt.Graphics2D;

/**
 * A class for objects in a SimulationWorld.
 *
 * The greenfoot.Actor class is less than ideal in multiple ways, and would not
 * function well with a SimulationWorld due to the nature of drawing graphics
 * directly onto the world's background image. Actors instead inherit from this
 * class.
 *
 * @author Martin Baldwin
 */
public abstract class SuperActor {
	protected SimulationWorld world;

	protected double x;
	protected double y;

	private boolean isDead;

	/**
	 * Create a new SuperActor in the given world at the given location.
	 *
	 * @param world the world to add this actor to
	 * @param x the x coordinate to place this actor at
	 * @param y the y coordinate to place this actor at
	 */
	public SuperActor(SimulationWorld world, double x, double y) {
		this.world = world;
		this.x = x;
		this.y = y;
		isDead = false;
	}

	/**
	 * Create a new SuperActor in the given world. Its location is set to (0.0, 0.0).
	 *
	 * @param world the world to add this actor to
	 */
	public SuperActor(SimulationWorld world) {
		this(world, 0.0, 0.0);
	}

	/**
	 * Mark this SuperActor as dead; to be removed from the world.
	 */
	public void die() {
		isDead = true;
	}

	/**
	 * Test if this SuperActor is marked dead.
	 *
	 * @return true if dead, false otherwise
	 */
	public boolean isDead() {
		return isDead;
	}

	/**
	 * Update this SuperActor.
	 */
	public abstract void act();

	/**
	 * Draw this SuperActor using a given graphics context.
	 *
	 * @param graphics the Graphics2D context on which to draw this SuperActor
	 */
	public abstract void drawUsingGraphics(Graphics2D graphics);
}
