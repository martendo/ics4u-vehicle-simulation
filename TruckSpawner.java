/**
 * A specialized spawner to spawn truck actors, then payload actors at a later
 * time in order to provide the visual effect of the trucks leading the
 * payloads.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class TruckSpawner extends RandomSpawner {
	private final SimulationWorld world;
	private final SuperPath path;
	private final int laneNum;

	/**
	 * Create a new payload spawner.
	 *
	 * @param world the world to spawn actors into
	 * @param path the path to attach actors to
	 * @param laneNum the index of the lane in the given path to attach actors to
	 */
	public TruckSpawner(SimulationWorld world, SuperPath path, int laneNum) {
		super(120, 480);
		this.path = path;
		this.world = world;
		this.laneNum = laneNum;
	}

	/**
	 * Spawn a new truck then payload.
	 */
	@Override
	public void run() {
		// First spawn a truck to lead the new payload
		Truck truck = new Truck();
		path.addTraveller(truck, laneNum);
		world.addActor(truck);
		// Spawn the payload following this truck at a later time
		Spawner payloadSpawner = new FixedSpawner((int) (Truck.IMAGE.getWidth() / truck.getSpeed()), 1) {
			@Override
			public void run() {
				// Remove this one-time spawner
				path.removeSpawner(this);
				world.removeSpawner(this);
				// It is rare but possible that the truck has already been killed -> don't spawn
				if (truck.isDead()) {
					return;
				}
				// Spawn the payload following the truck
				Payload payload;
				int roll = (int) (Math.random() * 5.0);
				if (roll == 0) {
					payload = new Bomb(truck);
				} else {
					payload = new Candy(truck);
				}
				path.addTraveller(payload, truck.getLaneNumber());
				world.addActor(payload);
				truck.attachPayload(payload);
			}
		};
		world.addSpawner(payloadSpawner);
		path.addSpawner(payloadSpawner);
	}
}
