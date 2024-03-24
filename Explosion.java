import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.AffineTransform;

/**
 * A visual element shown when a bomb path traveller explodes.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Explosion extends Effect {
	public static final BufferedImage IMAGE = new GreenfootImage("images/explosion.png").getAwtImage();

	// Number of acts to keep an explosion on-screen
	public static final int LIFESPAN = 10;

	// The shape defining the area where actors will be killed from an explosion
	private static final Ellipse2D HIT_SHAPE = new Ellipse2D.Double(-100, -100, 200, 200);

	public Explosion() {
		super(LIFESPAN);
	}

	@Override
	public BufferedImage getImage() {
		return IMAGE;
	}

	@Override
	public AffineTransform getImageTransform() {
		// Draw the image centered at this explosion's coordinates
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.translate(-IMAGE.getWidth() / 2.0, -IMAGE.getHeight() / 2.0);
		return transform;
	}

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		return transform.createTransformedShape(HIT_SHAPE);
	}
}
