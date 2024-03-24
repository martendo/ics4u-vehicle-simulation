/**
 * A visual element that only exists in the world for a set number of acts.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class Effect extends SuperActor {
	// Number of acts left until this effect is removed
	private int timer;

	/**
	 * Create a new effect actor.
	 *
	 * @param lifespan the number of acts to stay alive for
	 */
	public Effect(int lifespan) {
		super();
		timer = lifespan;
	}

	@Override
	public void act() {
		if (--timer <= 0) {
			die();
		}
	}
}
