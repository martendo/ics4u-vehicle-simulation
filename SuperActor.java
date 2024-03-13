import java.awt.image.BufferedImage;

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
	 * Set this SuperActor's location to the specified coordinates.
	 *
	 * @param x the x-coordinate to move this actor to
	 * @param y the y-coordinate to move this actor to
	 */
	public void setLocation(double x, double y) {
		this.x = x;
		this.y = y;
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
	 * This method is called when this SuperActor is added to the SimulationWorld.
	 */
	public void addedToWorld(SimulationWorld world) {}

	/**
	 * Update this SuperActor.
	 */
	public void act() {};

	/**
	 * Return the image of this SuperActor for drawing.
	 */
	public abstract BufferedImage getImage();

	/**
	 * Return the X position of the left side of this SuperActor's image if the
	 * center of its image were to be placed at its internal coordinates.
	 */
	public int getX() {
		return (int) x - (getImage().getWidth() / 2);
	}

	/**
	 * Return the Y position of the top side of this SuperActor's image if the
	 * center of its image were to be placed at its internal coordinates.
	 */
	public int getY() {
		return (int) y - (getImage().getHeight() / 2);
	}
}
