import java.awt.image.BufferedImage;
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
	private double x;
	private double y;
	private double angle;
	private boolean isDead;

	// A reference to the world this actor was added to
	private SimulationWorld world;

	// All other actors to be considered as a unit with this actor
	private Set<SuperActor> linkedActors;

	// The Z layer this actor appears on, relative to the paths that exist in its world
	private int layer;

	/**
	 * Create a new SuperActor.
	 */
	public SuperActor() {
		x = 0.0;
		y = 0.0;
		angle = 0.0;
		isDead = false;
		world = null;
		linkedActors = new HashSet<SuperActor>();
		layer = -1;
	}

	/**
	 * Set this actor's layer index to the given value.
	 */
	public void setLayer(int layer) {
		this.layer = layer;
	}

	/**
	 * Return this actor's current layer index.
	 */
	public int getLayer() {
		return layer;
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
	 * Set this actor's world and call its addedToWorld method.
	 */
	public void setWorld(SimulationWorld world) {
		this.world = world;
		addedToWorld(world);
	}

	/**
	 * Return the simulation world this actor is in, or null if it is not in a world.
	 */
	public SimulationWorld getWorld() {
		return world;
	}

	/**
	 * This method is called when this actor is added to the SimulationWorld.
	 *
	 * By default, do nothing.
	 *
	 * @param world the SimulationWorld that this actor was just added to
	 */
	protected void addedToWorld(SimulationWorld world) {}

	/**
	 * Update this SuperActor.
	 *
	 * By default, do nothing.
	 */
	public void act() {};

	/**
	 * Return the image of this actor for drawing.
	 */
	public abstract BufferedImage getImage();

	/**
	 * Get the transformation to apply to this actor's image when drawing it
	 * onto the world.
	 *
	 * By default, this transforms the image so that its midright point is
	 * placed at this actor's location point and that it is rotated about this
	 * actor's location point.
	 */
	public AffineTransform getImageTransform() {
		AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
		transform.rotate(angle);
		transform.translate(-getImage().getWidth(), -getImage().getHeight() / 2.0);
		return transform;
	}

	/**
	 * Return a shape object representing the area which this actor is
	 * considered to be occupying, in world space (already transformed).
	 *
	 * By default, throw an UnsupportedOperationException.
	 */
	public Shape getHitShape() {
		throw new UnsupportedOperationException("Attempted to access the hit shape of a SuperActor class that does not provide one");
	}
}
