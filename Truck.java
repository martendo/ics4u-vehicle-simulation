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

	// The distance behind another traveller at which to slow down
	public static final double SLOWDOWN_DISTANCE = Dessert.TRUCK_BED_LENGTH + 16.0;
	public static final int LENGTH = Dessert.TRUCK_BED_LENGTH + SPRITE.getWidth() + 16;

	// The area of a truck, with its midright point at the origin
	private static final Rectangle2D HIT_RECT = new Rectangle2D.Double(-SPRITE.getWidth(), -SPRITE.getHeight() / 2.0, SPRITE.getWidth(), SPRITE.getHeight());

	// The default or target speed of this truck
	private double originalSpeed;
	// The traveller which this truck is currently stuck behind
	private PathTraveller limitingTraveller;

	// The dessert actor that is following this truck
	private Dessert attachedDessert;

	public Truck() {
		super(Math.random() * (MAX_SPEED - MIN_SPEED) + MIN_SPEED);
		originalSpeed = getSpeed();
		limitingTraveller = null;
		attachedDessert = null;
		initImage();
	}

	public void attachDessert(Dessert dessert) {
		attachedDessert = dessert;
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
				traveller.dieAndKillLinked();
				hit = true;
			}
		}
		if (hit) {
			dieAndKillLinked();
			return;
		}

		updateSpeed();
		// If stuck behind another traveller, check if it is possible to change lanes
		if (limitingTraveller != null) {
			int newLane = -1;
			if (getLaneNumber() > 0 && canMoveToLane(getLaneNumber() - 1)) {
				newLane = getLaneNumber() - 1;
			} else if (getLaneNumber() < getPath().getLaneCount() - 1 && canMoveToLane(getLaneNumber() + 1)) {
				newLane = getLaneNumber() + 1;
			}
			if (newLane != -1) {
				moveToLane(newLane);
			}
		}
	}

	/**
	 * Test if this truck will have space if it moved to the requested lane.
	 *
	 * @param laneNum the index of the lane in this truck's path to test
	 * @return true if this truck can change lanes without moving into an existing traveller, false otherwise
	 */
	private boolean canMoveToLane(int laneNum) {
		double thisTravelled = getPath().getEquivalentDistanceInLane(getLaneNumber(), getDistanceTravelled(), laneNum);
		// Check if any travellers in the lane occupy the space where this truck would move
		for (PathTraveller traveller : getPath().getTravellersInLane(laneNum)) {
			double otherTravelled = traveller.getDistanceTravelled();
			if (otherTravelled > thisTravelled - LENGTH && otherTravelled - SLOWDOWN_DISTANCE < thisTravelled) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Update this truck's speed to stay behind any slower travellers that may
	 * be in front, and update the reference to the limiting traveller accordingly.
	 */
	private void updateSpeed() {
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
		if (attachedDessert != null) {
			attachedDessert.setSpeed(speed);
		}
	}

	/**
	 * Move this truck to the given lane along with all of its linked actors who
	 * are path travellers.
	 */
	@Override
	public void moveToLane(int newLane) {
		super.moveToLane(newLane);
		if (attachedDessert != null) {
			attachedDessert.moveToLane(newLane, getDistanceTravelled() - SPRITE.getWidth());
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
