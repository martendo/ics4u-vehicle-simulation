/**
 * A specialized spawner to spawn all types of PathTraveller objects,
 * particularly spawning truck actors followed by payload actors at a later time
 * in order to provide the visual effect of the trucks leading the payloads.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class TravellerSpawner extends RandomSpawner {
	private final SimulationWorld world;
	private final SuperPath path;
	private final int laneNum;

	/**
	 * Create a new path traveller spawner.
	 *
	 * @param world the world to spawn actors into
	 * @param path the path to attach actors to
	 * @param laneNum the index of the lane in the given path to attach actors to
	 */
	public TravellerSpawner(SimulationWorld world, SuperPath path, int laneNum) {
		super(90, 300);
		this.path = path;
		this.world = world;
		this.laneNum = laneNum;
	}

	/**
	 * Spawn a new path traveller (truck then payload or zapper).
	 */
	@Override
	public void run() {
		if ((int) (Math.random() * 16) == 0) {
			Zapper zapper = new Zapper();
			zapper.setLayer(world.getPathIndex(path));
			path.addTraveller(zapper, laneNum);
			world.addActor(zapper);
			return;
		}
		int roll = (int) (Math.random() * 5);
		// First spawn a truck to lead the new payload
		Truck.Color color;
		if (roll == 0) {
			color = Truck.Color.BROWN;
		} else if (roll == 1) {
			color = Truck.Color.BLUE;
		} else {
			color = Truck.Color.GREEN;
		}
		Truck truck = new Truck(color);
		truck.setLayer(world.getPathIndex(path));
		path.addTraveller(truck, laneNum);
		world.addActor(truck);
		// Spawn the payload following this truck at a later time
		Spawner payloadSpawner = new FixedSpawner((int) (truck.getImage().getWidth() / truck.getSpeed()), 1) {
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
				if (roll == 0) {
					payload = new Bomb(truck);
				} else if (roll == 1) {
					payload = new Poison(truck);
				} else {
					payload = new Candy(truck);
				}
				payload.setLayer(world.getPathIndex(path));
				path.addTraveller(payload, truck.getLaneNumber());
				world.addActor(payload);
				truck.attachPayload(payload);
			}
		};
		world.addSpawner(payloadSpawner);
		path.addSpawner(payloadSpawner);
	}
}
