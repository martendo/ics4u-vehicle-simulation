/**
 * An individual timer that counts down by act, calling its spawn method after a
 * fixed number of acts has passed.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class FixedSpawner extends Spawner {
	// The fixed time interval of this spawner
	private final int time;

	/**
	 * Create a new spawner that spawns at a fixed interval, spawning the specified number of times.
	 *
	 * @param time the time interval between spawns (in acts)
	 * @param spawnCount the number of times to spawn before finishing. If less than 1, this spawner will spawn indefinitely.
	 */
	public FixedSpawner(int time, int spawnCount) {
		super(spawnCount);
		this.time = time;
		resetTimer();
	}

	/**
	 * Create a new spawner that spawns at a fixed interval indefinitely.
	 *
	 * @param time the time interval between spawns (in acts)
	 */
	public FixedSpawner(int time) {
		this(time, 0);
	}

	/**
	 * Set up this spawner's timer with its fixed time interval.
	 */
	@Override
	protected void resetTimer() {
		setTimer(time);
	}
}
