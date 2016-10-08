package template;

import java.util.LinkedList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import template.MDPAction.Type;

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


public class ReactiveRLA implements ReactiveBehavior {
	
	public static final double DELTA_THRESH = 1e-6;  // error term for double comparison
	public static final int MAX_ITER = (int) 1e4;  // maximum loop
	
	private Random random;  // generator
	private int numActions;  // number of actions (not always possible)
	private Agent agent;
	private double discount;
	private Map<City, Map<MDPAction, Double>> Q;  // Q[state][action] = double
	private Map<State, Double> V;  // future value of each state, independent of action
	private Map<State, MDPAction> policy;  // offline policy
	private Map<City, List<State>> reachableStates; // for each city, the number of states is limited

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		System.out.println("Setup called");
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		
		this.random = new Random(0);
		this.numActions = 0;
		this.agent = agent;
		this.discount = discount;
		
		Vehicle vehicle = agent.vehicles().get(0);
		
		initReachableStates(topology);
		
		System.out.println("Learning MDP");
		learnMDP(topology, td, vehicle);
		computeOptimalPolicy(topology);
		System.out.println("Setup finished");
	}
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action = chooseBestAction(vehicle.getCurrentCity(), availableTask);
		
		if (numActions >= 1) {
			System.out.println(agent.name() + ": the total profit after " + numActions + 
					" actions is " + agent.getTotalProfit() + 
					" (average profit: " + (agent.getTotalProfit() / (double)numActions) + ")");
		}
		numActions++;
		
		return action;
	}
	
	// initialize once for all the possible states of each city 
	private void initReachableStates(Topology topology){
		reachableStates = new HashMap<City, List<State>>();
		
		for(City citySrc : topology){
			List<State> states = new LinkedList<State>();
			for(City cityPkg : topology)
				states.add(new State(citySrc, cityPkg));
			
			reachableStates.put(citySrc, states);
		}
	}
	
	private void initQ(Topology topology) {
		Q = new HashMap<City, Map<MDPAction, Double>>();

		for(City citySrc : topology)
			Q.put(citySrc, new HashMap<MDPAction, Double>());
		
		System.out.println("Q table initialized");
	}
	
	
	private void initV(Topology topology) {
		V = new HashMap<State, Double>();
		
		for(City citySrc : topology)
			for(City cityPkg : topology)
				V.put(new State(citySrc, cityPkg), random.nextDouble());
		
		System.out.println("V table initialized");
	}
	
	// It is possible to loop on the keys because maps have been initialized with the right keys.
	private void learnMDP(Topology topology, TaskDistribution td, Vehicle vehicle) {
		initQ(topology);
		initV(topology);
		
		double improve = 1.;
		
		int count = 0;
		
		while ((count < MAX_ITER) && (improve > DELTA_THRESH)){
			//loop for s in S
			for(State state : V.keySet()){
				Map<MDPAction, Double> Qs = Q.get(state.getSrc());
				
				double qmax = Double.NEGATIVE_INFINITY;
				
				City citySrc = state.getSrc();
				
				//loop for a in A
				for(MDPAction a : state.getActions()){
					
					City cityDest = a.getDest();
					
					double reward = 0;
					
					if (a.getType() == Type.PICKUP)
						reward = (double) td.reward(citySrc, cityDest);
					
					double cost = vehicle.costPerKm() * citySrc.distanceTo(cityDest);
								
					// the next city is determined by a
					// but the package in the next city has a distribution
					double v = 0;
					for(State stateNext : reachableStates.get(cityDest)){
						double proba = td.probability(cityDest, stateNext.getPkgDest());
						v += proba * V.get(stateNext);
					}
					
					// update Q = R + gamma sum(possible tasks) P(task)*V(T(s,a))
					double q = reward - cost + discount * v;
					
					Qs.put(a, q);
					
					if (q > qmax) 
						qmax = q;
				}
				
				// update Q
				Q.put(citySrc, Qs);
				
				// update V
				double valueOld = V.get(state);
				V.put(state, qmax);
				improve = Math.abs(valueOld - qmax);		
			}
			count++;
		}
		System.out.println("Q tables created");
	}
	
	private void computeOptimalPolicy(Topology topology){
		policy = new HashMap<State, MDPAction>();
		
		for(City citySrc : topology){
			for(City cityPkg : topology){
				
				State state = new State(citySrc, cityPkg);
				
				// compute an optimal policy based on Q-values
				
				double qmax = Double.NEGATIVE_INFINITY;
				MDPAction bestAction = null;
				double q;
				
				for(MDPAction a : state.getActions()){
					q = Q.get(citySrc).get(a);
					
					if (q > qmax) {
						qmax = q;
						bestAction = a;
					}
				}
				
				policy.put(state, bestAction);
			}
		}
	}
	
	//Used to choose the best MDPAction from the policy and turn it into an action
	//Necessary because the State class does not have a task attribute
	//This is due to the fact that it is possible to enumerate cities but not tasks
	private Action chooseBestAction(City citySrc, Task task){
		City cityPkg;
		
		if(task != null)
			cityPkg = task.deliveryCity;
		else
			cityPkg = citySrc;
		
		MDPAction mdpAction = policy.get(new State(citySrc, cityPkg));
		
		Action action = null;
		switch(mdpAction.getType()){
		case PICKUP:
			action = new Pickup(task);
			break;
			
		case MOVE:
			action = new Move(mdpAction.getDest());
			break;
		}
		
		return action;
	}

}
