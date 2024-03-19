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
	public static final double MIN_SPEED = 0.5;
	public static final double MAX_SPEED = 3.0;

	public static final BufferedImage SPRITE = new GreenfootImage("images/truck.png").getAwtImage();

	// The area of a truck, with its midright point at the origin
	private static final Rectangle2D HIT_RECT = new Rectangle2D.Double(-SPRITE.getWidth(), -SPRITE.getHeight() / 2.0, SPRITE.getWidth(), SPRITE.getHeight());

	public Truck() {
		super(Math.random() * (MAX_SPEED - MIN_SPEED) + MIN_SPEED);
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
			return;
		}

		// Check if this truck should slow down when stuck behind slower travellers
		for (PathTraveller traveller : getPath().getTravellersInLane(getLaneNumber())) {
			if (traveller.getSpeed() >= getSpeed()) {
				continue;
			}
			double distance = traveller.getDistanceTravelled() - getDistanceTravelled();
			if (distance < 0.0 || distance > Dessert.TRUCK_BED_LENGTH + 16.0) {
				continue;
			}
			// This truck is stuck behind a slower traveller
			// Update the speed of this truck and its linked travellers
			setSpeed(traveller.getSpeed());
			for (SuperActor actor : getLinkedActors()) {
				if (actor instanceof PathTraveller) {
					((PathTraveller) actor).setSpeed(traveller.getSpeed());
				}
			}
			break;
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
