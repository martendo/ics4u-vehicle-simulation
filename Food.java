/**
 * A category of payload types that birds will try to eat.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class Food extends Payload {
	public Food(Truck truck) {
		super(truck);
	}
}
