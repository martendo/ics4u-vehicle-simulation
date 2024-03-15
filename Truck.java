import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;

/**
 * The visual object that brings a dessert object along its path.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Truck extends PathTraveller {
	private static final BufferedImage SPRITE = new GreenfootImage("images/truck.png").getAwtImage();

	@Override
	protected BufferedImage getSprite() {
		return SPRITE;
	}
}
