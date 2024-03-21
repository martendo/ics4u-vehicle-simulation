import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

public class Bird extends Wanderer {
	public static final BufferedImage IMAGE = new GreenfootImage("images/bird.png").getAwtImage();

	private static final double MAX_ACCEL = 0.1;

	private double speed;
	private double accelMag;
	private double accelAngle;
	private boolean wasOutside;

	private Spawner accelChangeTimer;

	public Bird() {
		super();
		setLocation(SimulationWorld.WIDTH / 2.0, SimulationWorld.HEIGHT / 2.0);
		accelChangeTimer = new RandomSpawner(10, 60) {
			@Override
			public void run() {
				accelMag = Math.random() * MAX_ACCEL;
				accelAngle = Math.random() * Math.PI * 2.0;
			}
		};
	}

	@Override
	public void act() {
		accelChangeTimer.act();
		if (getX() < 0 || getX() > SimulationWorld.WIDTH || getY() < 0 || getY() > SimulationWorld.HEIGHT) {
			accelMag = MAX_ACCEL;
			accelAngle = Math.atan2(SimulationWorld.HEIGHT / 2.0 - getY(), SimulationWorld.WIDTH / 2.0 - getX());
			wasOutside = true;
		} else if (wasOutside) {
			accelMag = -accelMag;
			wasOutside = false;
		}
		double speedX = speed * Math.cos(getRotation()) + accelMag * Math.cos(accelAngle);
		double speedY = speed * Math.sin(getRotation()) + accelMag * Math.sin(accelAngle);
		speed = Math.hypot(speedX, speedY);
		setRotation(Math.atan2(speedY, speedX));
		setLocation(getX() + speedX, getY() + speedY);
	}

	@Override
	public BufferedImage getImage() {
		return IMAGE;
	}

	@Override
	public AffineTransform getImageTransform() {
		// Draw the image centered at and rotated around this bird's location
		AffineTransform transform = AffineTransform.getTranslateInstance(getX(), getY());
		transform.rotate(getRotation());
		transform.translate(-IMAGE.getWidth() / 2.0, -IMAGE.getHeight() / 2.0);
		return transform;
	}
}
