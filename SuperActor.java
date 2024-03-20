import greenfoot.util.GraphicsUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Shape;
import java.util.Set;
import java.util.HashSet;

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
	// Settings
	public static final boolean DEBUG_SHOW_IMAGE_BOXES = false;

	private double x;
	private double y;
	private double angle;
	private boolean isDead;

	// All other actors to be considered as a unit with this actor
	private Set<SuperActor> linkedActors;

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
		linkedActors = new HashSet<SuperActor>();
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
	 * Add an actor to this actor's set of linked actors and add this actor to
	 * the other actor's set of linked actors.
	 *
	 * @param actor the actor to link
	 */
	public void linkActor(SuperActor actor) {
		linkedActors.add(actor);
		if (!actor.getLinkedActors().contains(this)) {
			actor.linkActor(this);
		}
	}

	/**
	 * Remove an actor from this actor's set of linked actors and remove this
	 * actor from the other actor's set of linked actors.
	 *
	 * @param actor the actor to unlink
	 */
	public void unlinkActor(SuperActor actor) {
		linkedActors.remove(actor);
		if (actor.getLinkedActors().contains(this)) {
			actor.unlinkActor(this);
		}
	}

	/**
	 * Return the set of actors which have been linked to this actor.
	 */
	public Set<SuperActor> getLinkedActors() {
		return new HashSet<SuperActor>(linkedActors);
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
	 * Mark this actor as dead (to be removed from the world) and unlink it from
	 * all other actors.
	 */
	public void die() {
		isDead = true;
		for (SuperActor actor : linkedActors) {
			actor.unlinkActor(this);
		}
		linkedActors.clear();
	}

	/**
	 * Mark this actor and all of its linked actors as dead (to be removed from
	 * the world).
	 */
	public void dieAndKillLinked() {
		for (SuperActor actor : linkedActors) {
			actor.die();
		}
		die();
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
	 * By default, do nothing.
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
	 * Reset this actor's image then draw its sprite onto it.
	 */
	public void updateImage() {
		resetImage();
		drawSpriteToImage();
	}

	/**
	 * Draw this actor's sprite rotated to its current angle of rotation onto
	 * its image.
	 */
	public void drawSpriteToImage() {
		graphics.drawImage(getSprite(), getSpriteTransform(), null);
	}

	/**
	 * Return the necessary transformation to apply before drawing this actor's
	 * sprite onto its image.
	 */
	protected AffineTransform getSpriteTransform() {
		// Place this actor's sprite so its midright point is at the center of the image
		BufferedImage sprite = getSprite();
		AffineTransform transform = getCenterRotateTransform();
		transform.translate(-sprite.getWidth(), (double) -sprite.getHeight() / 2.0);
		return transform;
	}

	protected AffineTransform getCenterRotateTransform() {
		// Rotate from the center of this actor's image
		AffineTransform transform = AffineTransform.getTranslateInstance((double) image.getWidth() / 2.0, (double) image.getHeight() / 2.0);
		transform.rotate(angle);
		return transform;
	}

	/**
	 * Return the image of this actor for drawing.
	 */
	public BufferedImage getImage() {
		if (DEBUG_SHOW_IMAGE_BOXES) {
			graphics.setColor(java.awt.Color.WHITE);
			graphics.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
			graphics.fillRect(image.getWidth() / 2 - 5, image.getHeight() / 2 - 5, 10, 10);
		}
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

	/**
	 * Return a shape object representing the area which this actor is
	 * considered to be occupying, in world space (already transformed).
	 */
	public abstract Shape getHitShape();
}
