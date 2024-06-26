import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;

/**
 * A vehicle that will create an explosion after a random amount of time.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Bomb extends Payload {
	private static final BufferedImage sprite = new GreenfootImage("images/bomb.png").getAwtImage();

	private static final SoundEffect EXPLOSION_SOUND = new SoundEffect("sounds/explosion.wav");

	// Number of acts left until explosion
	private int timer;

	public Bomb(Truck truck) {
		super(truck);
	}

	@Override
	public void addedToPath(SuperPath path, int laneNum) {
		super.addedToPath(path, laneNum);
		timer = (int) (Math.random() * path.getLaneLength(laneNum) / getSpeed());
	}

	@Override
	public void act() {
		super.act();

		if (!hasItem()) {
			return;
		}
		if (--timer <= 0) {
			explode();
		}
	}

	private void explode() {
		// Create the explosion actor
		Explosion explosion = new Explosion();
		explosion.setLayer(getWorld().getPathIndex(getPath()));
		explosion.setLocation(getX(), getY());

		EXPLOSION_SOUND.play();

		// Kill all travellers within the range of the explosion shape (circle of radius 100)
		for (PathTraveller traveller : getPath().getTravellers()) {
			if (explosion.getHitShape().contains(traveller.getX(), traveller.getY())) {
				traveller.dieAndKillLinked();
			}
		}
		// Kill all birds in range on or below this explosion's layer
		for (Bird bird : getWorld().getActors(Bird.class, getLayer())) {
			if (explosion.getHitShape().contains(bird.getX(), bird.getY())) {
				bird.die();
			}
		}

		// Scare all UFOs away
		for (Ufo ufo : getWorld().getActors(Ufo.class)) {
			ufo.scareAwayFromPoint(getX(), getY());
		}

		getWorld().addActor(explosion);
	}

	@Override
	protected BufferedImage getSprite() {
		return sprite;
	}
}
