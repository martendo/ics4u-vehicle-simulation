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
 * @version March 2024
 */
public abstract class SuperActor {
	protected double x = 0.0;
	protected double y = 0.0;

	private boolean isDead;

	/**
	 * Create a new SuperActor.
	 */
	public SuperActor() {
		isDead = false;
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
