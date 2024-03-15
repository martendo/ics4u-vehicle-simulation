import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

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
	private double x;
	private double y;
	private double angle;
	private boolean isDead;

	// The image representing this actor to draw onto the world
	private BufferedImage image;
	// The graphics context for this actor's image
	private Graphics2D graphics;

	/**
	 * Create a new SuperActor.
	 */
	public SuperActor() {
		x = 0.0;
		y = 0.0;
		angle = 0.0;
		isDead = false;
		image = null;
		graphics = null;
	}

	/**
	 * Create this actor's image and graphics context using the image size
	 * returned by its getImageSize() method.
	 */
	public void initImage() {
		int size = getImageSize();
		image = GraphicsUtilities.createCompatibleTranslucentImage(size, size);
		graphics = image.createGraphics();
		graphics.addRenderingHints(SimulationWorld.RENDERING_HINTS);
		graphics.setBackground(new java.awt.Color(0, 0, 0, 0));
	}

	/**
	 * Set this actor's location to the specified coordinates.
	 *
	 * @param x the x-coordinate to move this actor to
	 * @param y the y-coordinate to move this actor to
	 */
	public void setLocation(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Return this actor's X position in double precision.
	 */
	public double getX() {
		return x;
	}

	/**
	 * Return this actor's Y position in double precision.
	 */
	public double getY() {
		return y;
	}

	/**
	 * Set the angle of rotation of this actor.
	 */
	public void setRotation(double angle) {
		this.angle = angle;
	}

	/**
	 * Return this actor's angle of rotation.
	 */
	public double getRotation() {
		return angle;
	}

	/**
	 * Mark this actor as dead; to be removed from the world.
	 */
	public void die() {
		isDead = true;
	}

	/**
	 * Test if this actor is marked dead.
	 *
	 * @return true if dead, false otherwise
	 */
	public boolean isDead() {
		return isDead;
	}

	/**
	 * This method is called when this actor is added to the SimulationWorld.
	 *
	 * @param world the SimulationWorld that this actor was just added to
	 */
	public void addedToWorld(SimulationWorld world) {}

	/**
	 * Update this SuperActor.
	 *
	 * By default, do nothing.
	 */
	public void act() {};

	/**
	 * Set this actor's image to its sprite image rotated to its current angle
	 * of rotation.
	 */
	public void updateImage() {
		resetImage();
		BufferedImage sprite = getSprite();
		// Rotate from the center of this actor's image
		AffineTransform transform = AffineTransform.getTranslateInstance((double) image.getWidth() / 2.0, (double) image.getHeight() / 2.0);
		transform.rotate(angle);
		// Place this actor's sprite so its midright point is at the center of the image
		transform.translate(-sprite.getWidth(), -sprite.getHeight() / 2);
		graphics.drawImage(sprite, transform, null);
	}

	/**
	 * Return the image of this actor for drawing.
	 */
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * Return this actor's graphics context.
	 */
	public Graphics2D getGraphics() {
		return graphics;
	}

	/**
	 * Retrieve the image to draw rotated for this actor's image.
	 */
	protected abstract BufferedImage getSprite();

	/**
	 * Retrieve the size to use to create this actor's image.
	 *
	 * By default, use double the largest dimension of this actor's sprite.
	 */
	protected int getImageSize() {
		BufferedImage sprite = getSprite();
		return Math.max(sprite.getWidth(), sprite.getHeight()) * 2;
	}

	/**
	 * Prepare this actor's image for drawing.
	 *
	 * By default, clear its image to transparency.
	 */
	protected void resetImage() {
		graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
	}

	/**
	 * Return the X position of the left side of this SuperActor's image if the
	 * center of its image were to be placed at its internal coordinates.
	 */
	public double getPreciseImageX() {
		return x - (double) image.getWidth() / 2.0;
	}

	/**
	 * Return the Y position of the top side of this SuperActor's image if the
	 * center of its image were to be placed at its internal coordinates.
	 */
	public double getPreciseImageY() {
		return y - (double) image.getHeight() / 2.0;
	}
}
