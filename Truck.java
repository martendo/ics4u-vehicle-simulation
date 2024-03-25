import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * The object that brings a payload object along its path, also dealing with its
 * lane changing and collision.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Truck extends Driver {
	public static final double MIN_SPEED = 1.5;
	public static final double MAX_SPEED = 3.0;

	// Number of acts to wait after changing lanes before changing again
	public static final int LANE_CHANGE_TIMEOUT = 120;

	// All types of trucks with different images
	public enum Color {
		GREEN("images/truck-green.png"),
		BLUE("images/truck-blue.png"),
		BROWN("images/truck-brown.png");

		public final BufferedImage image;

		private Color(String filename) {
			image = new GreenfootImage(filename).getAwtImage();
		}
	}

	// The distance behind another traveller at which to slow down
	public static final double SLOWDOWN_DISTANCE = Payload.TRUCK_BED_LENGTH + 24.0;
	public static final int LENGTH = Payload.TRUCK_BED_LENGTH + Color.GREEN.image.getWidth() + 24;

	// The area of a truck, with its midright point at the origin
	private static final Shape HIT_SHAPE = new Rectangle2D.Double(-35, -29 / 2.0, 35, 29);

	private static final SoundEffect HORN_SOUND = new SoundEffect("sounds/horn.wav");

	// The default or target speed of this truck
	private double originalSpeed;
	// The color of this truck, determining what image to use
	private Color color;

	// The traveller which this truck is currently stuck behind
	private PathTraveller limitingTraveller;

	// The payload actor that is following this truck
	private Payload attachedPayload;

	// Number of acts to wait until this truck can make another lane change
	private int laneChangeTimer;

	public Truck(Color color) {
		super(Math.random() * (MAX_SPEED - MIN_SPEED) + MIN_SPEED);
		originalSpeed = getSpeed();
		if (AlienInvasion.isActive()) {
			originalSpeed *= AlienInvasion.TRAVELLER_SPEED_FACTOR;
			setSpeed(originalSpeed);
		}
		this.color = color;
		limitingTraveller = null;
		attachedPayload = null;
		laneChangeTimer = 0;
	}

	public void attachPayload(Payload payload) {
		attachedPayload = payload;
	}

	@Override
	public void act() {
		super.act();
		if (isDead()) {
			return;
		}

		updateSpeed();

		// Update timer to decide if changing lanes is allowed
		if (laneChangeTimer > 0) {
			laneChangeTimer--;
		}
		// If stuck behind another traveller and lane change timeout is over, check if it is possible to change lanes
		if (limitingTraveller != null && laneChangeTimer == 0) {
			int newLane = -1;
			if (getLaneNumber() > 0 && canMoveToLane(getLaneNumber() - 1)) {
				newLane = getLaneNumber() - 1;
			} else if (getLaneNumber() < getPath().getLaneCount() - 1 && canMoveToLane(getLaneNumber() + 1)) {
				newLane = getLaneNumber() + 1;
			}
			if (newLane != -1) {
				moveToLane(newLane);
				// Wait until making another lane change
				if (getLaneNumber() == newLane) {
					HORN_SOUND.play();
					laneChangeTimer = LANE_CHANGE_TIMEOUT;
				}
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
		double thisTravelled = getPath().getAdjacentDistanceInLane(getLaneNumber(), getDistanceTravelled(), laneNum);
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
		if (attachedPayload != null) {
			attachedPayload.setSpeed(speed);
		}
	}

	/**
	 * Move this truck to the given lane along with all of its linked actors who
	 * are path travellers.
	 */
	@Override
	public void moveToLane(int newLane) {
		// If this truck's payload would be moved to part of the path that doesn't exist
		// (negative distance), don't make the lane change
		double newDistance = getPath().getAdjacentDistanceInLane(getLaneNumber(), getDistanceTravelled(), newLane);
		if (attachedPayload != null && newDistance - getImage().getWidth() < 0.0) {
			return;
		}
		super.moveToLane(newLane, newDistance);
		if (attachedPayload != null) {
			attachedPayload.moveToLane(newLane, getDistanceTravelled() - getImage().getWidth());
		}
	}

	@Override
	public BufferedImage getImage() {
		return color.image;
	}

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		return transform.createTransformedShape(HIT_SHAPE);
	}
}
