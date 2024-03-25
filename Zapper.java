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
public class Zapper extends Driver {
	public static final BufferedImage image = new GreenfootImage("images/zapper.png").getAwtImage();

	public static final double SPEED = 0.5;

	private static final Shape HIT_SHAPE = new Rectangle2D.Double(-image.getWidth(), -image.getHeight() / 2.0, image.getWidth(), image.getHeight());

	private static final SoundEffect ZAP_SOUND = new SoundEffect("sounds/zap.wav");

	// Number of acts to wait between zapping UFOs
	private static final int ZAP_INTERVAL = 600;

	// Number of acts until the next attempt to zap a UFO
	private int zapTimer;

	public Zapper() {
		super(AlienInvasion.isActive() ? SPEED * AlienInvasion.TRAVELLER_SPEED_FACTOR : SPEED);
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
		if (zapTimer != 0) {
			return;
		}
		// Find the closest UFO to zap it
		Ufo closestUfo = null;
		double minDistance = 0.0;
		for (Ufo ufo : getWorld().getActors(Ufo.class)) {
			// Don't consider this UFO if it is just outside of the world
			if (ufo.getX() < 0 || ufo.getX() > SimulationWorld.WIDTH || ufo.getY() < 0 || ufo.getY() > SimulationWorld.HEIGHT) {
				continue;
			}
			// When there is a path in between this UFO and this zapper on the Z axis, can't zap
			if (getWorld().getPathUnderPoint(getX(), getY(), ufo.getLayer()) != getPath()) {
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
		ZAP_SOUND.play();
		// Create the zap effect
		double centerX = getX() - image.getWidth() / 2.0 * Math.cos(getRotation());
		double centerY = getY() - image.getWidth() / 2.0 * Math.sin(getRotation());
		Zap zap = new Zap(centerX, centerY, closestUfo.getX(), closestUfo.getY());
		zap.setLayer(closestUfo.getLayer());
		getWorld().addActor(zap);
		// Reset the timer
		zapTimer = ZAP_INTERVAL;
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	@Override
	public Shape getHitShape() {
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		return transform.createTransformedShape(HIT_SHAPE);
	}
}
