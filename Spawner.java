/**
 * An individual timer that counts down by act, calling its spawn method after a
 * number of acts defined by its resetTImer() method has passed.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class Spawner {
	// Number of acts until next spawn
	private int timer;
	// Number of spawns left until this spawner is exhausted
	private int spawnsLeft;
	// Whether or not this spawner is exhausted
	private boolean isDone;

	/**
	 * Create a new spawner that will spawn the specified number of times.
	 *
	 * @param spawnCount the number of times to spawn before finishing. If less than 1, this spawner will spawn indefinitely.
	 */
	public Spawner(int spawnCount) {
		if (spawnCount < 1) {
			spawnsLeft = -1;
		} else {
			spawnsLeft = spawnCount;
		}
		isDone = false;
	}

	/**
	 * Set this spawner's timer to the specified value.
	 *
	 * @param value the value to give this spawner's timer (in acts)
	 */
	protected void setTimer(int value) {
		timer = value;
	}

	/**
	 * Set up this spawner's timer for the next time interval.
	 */
	protected abstract void resetTimer();

	/**
	 * Update this spawner's timer and call its spawn method when it is time.
	 * After this spawner's last spawn has been made, it will call the die() method.
	 */
	public void act() {
		if (isDone || --timer > 0) {
			return;
		}
		run();
		if (spawnsLeft < 0 || --spawnsLeft > 0) {
			resetTimer();
			return;
		}
		die();
	}

	/**
	 * Mark this spawner as exhausted.
	 */
	private void die() {
		isDone = true;
	}

	/**
	 * Test if this spawner has made its requested number of spawns.
	 */
	public boolean isDone() {
		return isDone;
	}

	/**
	 * This method is called at the end of each spawn time interval.
	 */
	public abstract void run();
}
