import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

/**
 * The visual object that brings a dessert object along its path.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Truck extends PathTraveller {
	public static final double SPEED = 2.0;

	public static final BufferedImage SPRITE = new GreenfootImage("images/truck.png").getAwtImage();

	public Truck() {
		super(SPEED);
	}

	@Override
	protected BufferedImage getSprite() {
		return SPRITE;
	}
}
