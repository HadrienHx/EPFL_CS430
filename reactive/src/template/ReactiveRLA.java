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

	public static final double DELTA_THRESH = 0.00000000001;
	public static final double MAX_ITER = Double.POSITIVE_INFINITY; 
	
	private Random random;
	private int numActions;
	private Agent myAgent;
	private Map<City,Map<MDPAction,Double>> Q;
	private Map<State,Double> V;
	private Map<State,MDPAction> policy;
	private double discount;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		System.out.println("Setup called");
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		
		this.random = new Random();
		this.numActions = 0;
		this.myAgent = agent;
		this.discount = discount;
		
		Vehicle vehicle = agent.vehicles().get(0);
		
		System.out.println("Learning Q");
		learnQ(topology,td,vehicle);
		policy = computeOptimalPolicy(topology);
		System.out.println("Setup finished");
//		System.out.println(Q);
//		System.out.println("V");
//		System.out.println(V);
//		System.out.println("Policy");
//		System.out.println(policy);
	}

	
	//Code here the learning of Q
	//Value iteration
	
	private Map<City,List<State>> getReachableStates(Topology topology){
		Map<City,List<State>> reachableStates = new HashMap<City,List<State>>();
		
		for(City c : topology){
			List<State> states = new LinkedList<State>();
			for(City d : topology){
				states.add(new State(c,d));
			}
			reachableStates.put(c, states);
		}
		return reachableStates;
	}
	
	//It is possible to loop on the keys because maps have been initialized with the right keys.
	private void learnQ(Topology topology, TaskDistribution td, Vehicle vehicle) {
		Q = initQ(topology);
		V = initV(topology);
		Map<City,List<State>> reachableStates = getReachableStates(topology);
		
		double deltaV = 1.;
		
		int count = 0;
		
		while ((count < MAX_ITER) && (deltaV > DELTA_THRESH)){
			//loop for s in S
			deltaV = 0;
			for(State s : V.keySet()){
				Map<MDPAction,Double> Qs = Q.get(s.getCity());
				double qmax = Double.NEGATIVE_INFINITY;
				
				//loop for a in A
				for(MDPAction a : s.getActions()){
					int reward = 0;
					
					if (a.getType() == Type.PICKUP){
						reward = s.getReward(td);
					}
					double cost = vehicle.costPerKm() * s.getCity().distanceTo(a.getDestination());
					//update Q = R + gamma sum(possible tasks) P(task)*V(T(s,a))
					
					double v = 0;
					
					for(State nextState : reachableStates.get(a.getDestination())){
						double proba = td.probability(nextState.getCity(), nextState.getDestination());
						v = v + proba*V.get(nextState);
					}
//					System.out.println(s);
//					System.out.println(a);
//					System.out.println(reward + ", " + cost + ", " + v);
					double q = reward - cost + discount * v;
//					System.out.println(q);
					
					Qs.put(a, q);
					if(q > qmax) {
						qmax = q;
					}
				}
				Q.put(s.getCity(),Qs);
				//update V
				double previousValue = V.get(s);
				V.put(s, qmax);
				deltaV = Math.max(deltaV, Math.abs(previousValue - qmax));		
			}
			count++;
		}
		System.out.println("Q tables created");
	}
	
	private Map<City,Map<MDPAction,Double>> initQ(Topology topology) {
		Map<City,Map<MDPAction,Double>> Q = new HashMap<City,Map<MDPAction,Double>>();

		for(City c1 : topology){
			HashMap<MDPAction,Double> temp =  new HashMap<MDPAction,Double>();
			Q.put(c1, temp);
		}
		System.out.println("Q table initialized");
		return Q;
	}
	
	
	private Map<State,Double> initV(Topology topology) {
		Map<State,Double> V = new HashMap<State,Double>();
		
		double newRand;

		for(City c1 : topology){
			for(City c2: topology){
				
				State s = new State(c1,c2);
				newRand = random.nextDouble();
				
				V.put(s, newRand);
			}
		}
		return V;
	}
	
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action = chooseBestAction(vehicle.getCurrentCity(), availableTask);
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
	private Map<State,MDPAction> computeOptimalPolicy(Topology topology){
		Map<State,MDPAction> policy = new HashMap<State, MDPAction>();
		
		for(City city : topology){
			for(City destination : topology){
				State currentState = new State(city,destination);
				policy.put(currentState, computeBestAction(currentState));
			}
		}
		
		return policy;
	}
	
	//Used to choose the best MDPAction from the policy and turn it into an action
	//Necessary because the State class does not have a task attribute
	//This is due to the fact that it is possible to enumerate cities but not tasks
	private Action chooseBestAction(City currentCity, Task task){
		City destination = null;
		if(task != null){
			destination = task.deliveryCity;
		}
		State state = new State(currentCity, destination);
		
		MDPAction mdpa = policy.get(state);
		
		Action a = null;
		switch(mdpa.getType()){
		case PICKUP:
			a = new Pickup(task);
			break;
			
		case MOVE:
			a = new Move(mdpa.getDestination());
			break;
		}
		
		return a;
	}
	
	//Used to compute an optimal policy based on Q-values
	private MDPAction computeBestAction(State state){
		City currentCity = state.getCity();
		
		double maxVal = Double.NEGATIVE_INFINITY;
		MDPAction bestAction = null;
		
		Map<MDPAction, Double> potentialActions = Q.get(currentCity);
		List<MDPAction> actions = state.getActions();
		double q;
		
		for(MDPAction a : actions){
			q = potentialActions.get(a);
			
			if (q > maxVal){
				maxVal = q;
				bestAction = a;
			}
		}
		
		return bestAction;
	}
}
