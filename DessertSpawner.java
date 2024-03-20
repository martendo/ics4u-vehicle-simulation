/**
 * A specialized spawner to spawn truck actors, then dessert actors at a later
 * time in order to provide the visual effect of the trucks leading the
 * desserts.
 *
 * @author Martin Baldwin
 * @version March 2024
 */
public class DessertSpawner extends RandomSpawner {
	private final SimulationWorld world;
	private final SuperPath path;
	private final int laneNum;

	/**
	 * Create a new dessert spawner.
	 *
	 * @param world the world to spawn actors into
	 * @param path the path to attach actors to
	 * @param laneNum the index of the lane in the given path to attach actors to
	 */
	public DessertSpawner(SimulationWorld world, SuperPath path, int laneNum) {
		super(120, 480);
		this.path = path;
		this.world = world;
		this.laneNum = laneNum;
	}

	/**
	 * Spawn a new truck then dessert.
	 */
	@Override
	public void run() {
		// First spawn a truck to lead the new dessert
		Truck truck = new Truck();
		path.addTraveller(truck, laneNum);
		world.addActor(truck);
		// Spawn the dessert following this truck at a later time
		Spawner dessertSpawner = new FixedSpawner((int) (Truck.IMAGE.getWidth() / truck.getSpeed()), 1) {
			@Override
			public void run() {
				// Remove this one-time spawner
				path.removeSpawner(this);
				world.removeSpawner(this);
				// It is rare but possible that the truck has already been killed -> don't spawn
				if (truck.isDead()) {
					return;
				}
				// Spawn the dessert following the truck
				Dessert dessert;
				int roll = (int) (Math.random() * 5.0);
				if (roll == 0) {
					dessert = new Bomb(truck);
				} else {
					dessert = new Candy(truck);
				}
				path.addTraveller(dessert, truck.getLaneNumber());
				world.addActor(dessert);
				truck.attachDessert(dessert);
			}
		};
		world.addSpawner(dessertSpawner);
		path.addSpawner(dessertSpawner);
	}
}
