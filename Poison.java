import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;

/**
 * A type of food that birds will try to eat but get sick from.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Poison extends Food {
	public static final BufferedImage sprite = new GreenfootImage("images/poison.png").getAwtImage();

	public Poison(Truck truck) {
		super(truck);
	}

	@Override
	protected BufferedImage getSprite() {
		return sprite;
	}
}
