import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;

/**
 * The only type of dessert for now...
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Candy extends Payload {
	public static final BufferedImage SPRITE = new GreenfootImage("images/candy.png").getAwtImage();

	public Candy(Truck truck) {
		super(truck);
	}

	@Override
	protected BufferedImage getSprite() {
		return SPRITE;
	}
}
