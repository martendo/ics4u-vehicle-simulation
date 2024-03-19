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

	// The distance behind another traveller at which to slow down
	public static final double SLOWDOWN_DISTANCE = Dessert.TRUCK_BED_LENGTH + 16.0;

	public static final BufferedImage SPRITE = new GreenfootImage("images/truck.png").getAwtImage();

	// The area of a truck, with its midright point at the origin
	private static final Rectangle2D HIT_RECT = new Rectangle2D.Double(-SPRITE.getWidth(), -SPRITE.getHeight() / 2.0, SPRITE.getWidth(), SPRITE.getHeight());

	// The default or target speed of this truck
	private double originalSpeed;
	// The traveller which this truck is currently stuck behind
	private PathTraveller limitingTraveller;

	public Truck() {
		super(Math.random() * (MAX_SPEED - MIN_SPEED) + MIN_SPEED);
		originalSpeed = getSpeed();
		limitingTraveller = null;
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

		// Check if this truck should slow down behind a slower traveller when it is not currently
		// stuck behind one or the one it was stuck behind is no longer present in front
		if (limitingTraveller == null || (limitingTraveller.isDead() ||
		limitingTraveller.getLaneNumber() != getLaneNumber() ||
		limitingTraveller.getDistanceTravelled() - getDistanceTravelled() > SLOWDOWN_DISTANCE)) {
			limitingTraveller = null;
			// Check if there is a new traveller to be stuck behind
			for (PathTraveller traveller : getPath().getTravellersInLane(getLaneNumber())) {
				if (traveller.getSpeed() >= getSpeed()) {
					continue;
				}
				double distance = traveller.getDistanceTravelled() - getDistanceTravelled();
				if (distance > 0.0 && distance <= SLOWDOWN_DISTANCE) {
					limitingTraveller = traveller;
					break;
				}
			}
		}
		if (limitingTraveller != null) {
			// This truck is stuck behind a slower traveller
			setSpeed(limitingTraveller.getSpeed());
		} else if (getSpeed() < originalSpeed) {
			// There is no traveller ahead and this truck can go faster
			setSpeed(originalSpeed);
		}
	}

	/**
	 * Set the speed of this truck and all of its linked actors who are path travellers.
	 */
	@Override
	public void setSpeed(double speed) {
		super.setSpeed(speed);
		for (SuperActor actor : getLinkedActors()) {
			if (actor instanceof PathTraveller) {
				((PathTraveller) actor).setSpeed(speed);
			}
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
