import greenfoot.GreenfootSound;
import java.util.List;
import java.util.ArrayList;

/**
 * A manager of multiple GreenfootSound objects in order to allow many instances
 * of the same sound to be played concurrently. Also provides a means to pause
 * and resume all sounds that exist.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class SoundEffect {
	// Default maximum number of instances of a sound that can be played at once for each instance of SoundEffect
	public static final int DEFAULT_CONCURRENT_COUNT = 24;

	// All GreenfootSound objects that were ever created from SoundEffect objects
	private static List<GreenfootSound> allSounds = new ArrayList<GreenfootSound>();
	// All sounds that were previously paused by a call to pauseAllSounds(), kept in order to resume them
	private static List<GreenfootSound> pausedSounds = new ArrayList<GreenfootSound>();

	// The sound objects for use with this sound effect
	private GreenfootSound[] sounds;
	// The index into the sounds array of the next sound object to use
	private int nextSoundIndex;

	/**
	 * Create a new sound effect to play the given sound file, allowing a
	 * maximum number of instances of it to be played at once.
	 *
	 * @param filename the name of the sound file to play
	 * @param concurrentCount the maximum number of concurrently playing instances of this sound to account for
	 */
	public SoundEffect(String filename, int concurrentCount) {
		if (concurrentCount < 1) {
			throw new IllegalArgumentException("Must allow at least 1 instance of a sound effect to be played");
		}

		// Create an array of GreenfootSounds to play this sound effect
		sounds = new GreenfootSound[concurrentCount];
		for (int i = 0; i < concurrentCount; i++) {
			GreenfootSound sound = new GreenfootSound(filename);
			sounds[i] = sound;
			// Keep track of all sounds for pausing and resuming
			allSounds.add(sound);
		}
		nextSoundIndex = 0;
	}

	/**
	 * Create a new sound effect to play the given sound file.
	 *
	 * The maximum number of instances that can be played at once is determined
	 * by the SoundEffect.DEFAULT_CONCURRENT_COUNT constant.
	 *
	 * @param filename the name of the sound file to play
	 */
	public SoundEffect(String filename) {
		this(filename, DEFAULT_CONCURRENT_COUNT);
	}

	/**
	 * Play this sound effect from the beginning, on top of other instances of
	 * this sound effect when possible.
	 */
	public void play() {
		GreenfootSound sound = sounds[nextSoundIndex++];
		sound.stop(); // Stop to force this sound to be played from the beginning
		sound.play();

		if (nextSoundIndex >= sounds.length) {
			nextSoundIndex = 0;
		}
	}

	/**
	 * Pause all currently playing sounds that exist.
	 */
	public static void pauseAllSounds() {
		for (GreenfootSound sound : allSounds) {
			if (sound.isPlaying()) {
				sound.pause();
				pausedSounds.add(sound);
			}
		}
	}

	/**
	 * Resume all sounds that were previously paused by a call to pauseAllSounds().
	 */
	public static void resumeAllSounds() {
		for (GreenfootSound sound : pausedSounds) {
			sound.play();
		}
		pausedSounds.clear();
	}
}
