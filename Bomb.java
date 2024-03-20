import greenfoot.GreenfootImage;
import java.awt.image.BufferedImage;

/**
 * A vehicle that will create an explosion after a random amount of time.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class Bomb extends Dessert {
	public static final BufferedImage SPRITE = new GreenfootImage("images/bomb.png").getAwtImage();

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
		if (--timer <= 0) {
			explode();
		}
	}

	private void explode() {
		// Create the explosion actor
		Explosion explosion = new Explosion();
		explosion.setLocation(getX(), getY());
		getWorld().addActor(explosion);

		// Kill all travellers within the range of the explosion shape (circle of radius 100)
		for (PathTraveller traveller : getPath().getTravellers()) {
			if (explosion.getHitShape().contains(traveller.getX(), traveller.getY())) {
				traveller.dieAndKillLinked();
			}
		}
		dieAndKillLinked();
	}

	@Override
	protected BufferedImage getSprite() {
		return SPRITE;
	}
}
