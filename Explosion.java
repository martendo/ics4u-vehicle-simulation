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
public class Explosion extends SuperActor {
	public static final BufferedImage IMAGE = new GreenfootImage("images/explosion.png").getAwtImage();

	// Number of acts to keep an explosion on-screen
	public static final int LIFESPAN = 30;

	// The shape defining the area where actors will be killed from an explosion
	private static final Ellipse2D HIT_SHAPE = new Ellipse2D.Double(-100, -100, 200, 200);

	// Number of acts left until this explosion is removed
	private int timer;

	public Explosion() {
		super();
		timer = LIFESPAN;
	}

	@Override
	public void act() {
		if (--timer <= 0) {
			die();
		}
	}

	@Override
	public BufferedImage getImage() {
		return IMAGE;
	}

	@Override
	public AffineTransform getImageTransform() {
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