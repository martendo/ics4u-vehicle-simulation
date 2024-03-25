import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;

/**
 * A tasty type of food that birds will target and eat.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Candy extends Food {
	private static final BufferedImage sprite = new GreenfootImage("images/candy.png").getAwtImage();

	public Candy(Truck truck) {
		super(truck);
	}

	@Override
	protected BufferedImage getSprite() {
		return sprite;
	}
}
