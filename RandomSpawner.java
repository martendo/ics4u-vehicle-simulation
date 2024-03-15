/**
 * An individual timer that counts down by act, calling its spawn method after a
 * random number of acts within a specified range has passed.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public abstract class RandomSpawner extends Spawner {
	// The range of time intervals within which this spawner can randomly choose to wait between spawns
	private final int minTime;
	private final int maxTime;

	/**
	 * Create a new spawner that spawns at random intervals, spawning the specified number of times.
	 *
	 * @param minTime the shortest time interval between spawns (in acts)
	 * @param maxTime the longest time interval between spawns (in acts)
	 * @param spawnCount the number of times to spawn before finishing. If less than 1, this spawner will spawn indefinitely.
	 */
	public RandomSpawner(int minTime, int maxTime, int spawnCount) {
		super(spawnCount);
		this.minTime = minTime;
		this.maxTime = maxTime;
		resetTimer();
	}

	/**
	 * Create a new spawner that spawns at random intervals indefinitely.
	 *
	 * @param minTime the shortest time interval between spawns (in acts)
	 * @param maxTime the longest time interval between spawns (in acts)
	 */
	public RandomSpawner(int minTime, int maxTime) {
		this(minTime, maxTime, 0);
	}

	/**
	 * Set up this spawner's timer with a random number within its time interval range.
	 */
	@Override
	protected void resetTimer() {
		setTimer((int) (Math.random() * (maxTime - minTime)) + minTime);
	}
}
