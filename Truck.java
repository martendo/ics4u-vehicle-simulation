import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * The visual object that brings a dessert object along its path.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Truck extends PathTraveller {
	public static final double SPEED = 2.0;

	public static final BufferedImage SPRITE = new GreenfootImage("images/truck.png").getAwtImage();

	// The area of a truck, with its midright point at the origin
	private static final Rectangle2D HIT_RECT = new Rectangle2D.Double(-SPRITE.getWidth(), -SPRITE.getHeight() / 2.0, SPRITE.getWidth(), SPRITE.getHeight());

	public Truck() {
		super(SPEED);
		initImage();
	}

	@Override
	public void act() {
		super.act();
		// Crash into any other intersecting travellers on this path
		boolean hit = false;
		for (PathTraveller traveller : getPath().getTravellers()) {
			// Impossible to crash into itself or linked actors
			if (traveller == this || traveller.getLinkedActors().contains(this)) {
				continue;
			}
			// NOTE: This only tests a single point for collision for the sake of speed
			// (intersection of two transformed shapes causes noticeable slowdown)
			if (traveller.getHitShape().contains(getX(), getY())) {
				traveller.die();
				hit = true;
			}
		}
		if (hit) {
			die();
		}
	}

	@Override
	protected BufferedImage getSprite() {
		return SPRITE;
	}

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		return transform.createTransformedShape(HIT_RECT);
	}
}
