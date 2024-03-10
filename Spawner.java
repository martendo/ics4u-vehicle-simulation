/**
 * An individual timer that counts down by act, calling its spawn method after a
 * random number of acts within a specified range has passed.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class Spawner {
	private int minTime;
	private int maxTime;
	private int timer;

	/**
	 * Create a new spawner.
	 *
	 * @param minTime the shortest time interval between spawns (in acts)
	 * @param maxTime the longest time interval between spawns (in acts)
	 */
	public Spawner(int minTime, int maxTime) {
		this.minTime = minTime;
		this.maxTime = maxTime;
		resetTimer();
	}

	/**
	 * Set this spawner's timer to a new random time within its time range.
	 */
	private void resetTimer() {
		timer = (int) (Math.random() * (maxTime - minTime)) + minTime;
	}

	/**
	 * Update this spawner's timer and call its spawn method when it is time.
	 */
	public void act() {
		if (--timer > 0) {
			return;
		}
		spawn();
		resetTimer();
	}

	/**
	 * This method is called at a random time interval within this spawner's time range.
	 */
	public abstract void spawn();
}
