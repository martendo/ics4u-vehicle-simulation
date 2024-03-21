/**
 * A class for various types of wanderers (this project's version of
 * pedestrians) to inherit from.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class Wanderer extends SuperActor {
	// The distance to move in the direction of rotation per act
	private double speed;

	/**
	 * Set the speed of this wanderer to the given value.
	 */
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * Return this wanderer's current speed.
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Move this wanderer in its current direction by its current speed.
	 */
	@Override
	public void act() {
		double angle = getRotation();
		setLocation(getX() + speed * Math.cos(angle), getY() + speed * Math.sin(angle));
	}
}
