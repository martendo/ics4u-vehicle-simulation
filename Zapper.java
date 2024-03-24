import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * A rare vehicle that kills UFOs by zapping them after a fixed interval of time.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Zapper extends PathTraveller {
	public static final BufferedImage IMAGE = new GreenfootImage("images/zapper.png").getAwtImage();

	public static final double SPEED = 0.5;

	private static final Rectangle2D HIT_RECT = new Rectangle2D.Double(-IMAGE.getWidth(), -IMAGE.getHeight() / 2.0, IMAGE.getWidth(), IMAGE.getHeight());

	// Number of acts to wait between zapping UFOs
	private static final int ZAP_INTERVAL = 600;

	// Number of acts until the next attempt to zap a UFO
	private int zapTimer;

	public Zapper() {
		super(SPEED);
		zapTimer = ZAP_INTERVAL;
	}

	@Override
	public void act() {
		super.act();
		if (isDead()) {
			return;
		}

		if (zapTimer > 0) {
			zapTimer--;
		}
		if (zapTimer != 0 || getWorld().getPathUnderPoint(getX(), getY()) != getPath()) {
			return;
		}
		// Find the closest UFO to zap it
		Ufo closestUfo = null;
		double minDistance = 0.0;
		for (Ufo ufo : getWorld().getActors(Ufo.class)) {
			if (ufo.getX() < 0 || ufo.getX() > SimulationWorld.WIDTH || ufo.getY() < 0 || ufo.getY() > SimulationWorld.HEIGHT) {
				continue;
			}
			double distance = Math.hypot(ufo.getX() - getX(), ufo.getY() - getY());
			if (distance < minDistance || closestUfo == null) {
				closestUfo = ufo;
				minDistance = distance;
			}
		}
		if (closestUfo == null) {
			return;
		}
		// Zap this UFO
		closestUfo.die();
		// Create the zap effect
		double centerX = getX() - IMAGE.getWidth() / 2.0 * Math.cos(getRotation());
		double centerY = getY() - IMAGE.getWidth() / 2.0 * Math.sin(getRotation());
		Zap zap = new Zap(centerX, centerY, closestUfo.getX(), closestUfo.getY());
		zap.setLayer(getWorld().getPathCount() - 1);
		getWorld().addActor(zap);
		// Reset the timer
		zapTimer = ZAP_INTERVAL;
	}

	@Override
	public BufferedImage getImage() {
		return IMAGE;
	}

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		return transform.createTransformedShape(HIT_RECT);
	}
}
