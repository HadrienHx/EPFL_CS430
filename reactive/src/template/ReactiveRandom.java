package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveRandom implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent agent;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		Double pickup = agent.readProperty("pickup-rate", Double.class, 0.85);

		this.random = new Random();
		this.pPickup = pickup;
		this.numActions = 0;
		this.agent = agent;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println(agent.name() + ": the total profit after " + numActions + 
					" actions is " + agent.getTotalProfit() + 
					" (average profit: " + (agent.getTotalProfit() / (double)numActions) + ")");
		}
		numActions++;
		
		return action;
	}
}
