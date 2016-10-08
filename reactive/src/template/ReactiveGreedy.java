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

public class ReactiveGreedy implements ReactiveBehavior {

	private Random random;
	private int numActions;
	private Agent agent;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.random = new Random();
		this.numActions = 0;
		this.agent = agent;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		
		City currentCity = vehicle.getCurrentCity();
		
		if (availableTask == null)  // nothing else to do
			action = new Move(currentCity.randomNeighbor(random));
		else {
			
			double currentProfit = (agent.getTotalProfit() / (numActions + 1.0));
			double reward = availableTask.reward - currentCity.distanceTo(availableTask.deliveryCity);
			double nextProfit = (agent.getTotalProfit() + reward) / (numActions + 2.0);
			
			if (nextProfit > currentProfit)  // if it improves the profit, then pick up
				action = new Pickup(availableTask);
			else
				action = new Move(currentCity.randomNeighbor(random));
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
