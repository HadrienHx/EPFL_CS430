package template;

import java.util.LinkedList;
import java.util.List;

import template.MDPAction.Type;
import logist.task.TaskDistribution;
import logist.topology.Topology.City;

public class State {
	private City city;
	private City destination;
	
	public State(City c, City d){
		city = c;
		
		//If the task is for the same city it is assumed there is no task at all. 
		if (c.equals(d)){
			destination = null;
		} else{
		 destination = d;
		}	
	}
	
	public City getCity(){
		return city;
	}
	
	public City getDestination(){
		return destination;
	}
	
	@Override
	public int hashCode(){
		if (destination != null){
			return city.hashCode() * 11 * destination.hashCode();
		}
		return city.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if(!city.equals(((State) o).city)) 
			return false;
		
		if (destination == null)
			return ((State) o).destination == null;
		
		return destination.equals(((State) o).destination);
	}
	
	public int getReward(TaskDistribution td){
		if (destination == null) return 0;
		return td.reward(city, destination);
	}
	
	public List<MDPAction> getActions(){
		List<MDPAction> actions = new LinkedList<MDPAction>();
		
		//Only moves to nearby cities count (because if the city is not a neighbor trajectory is cut in 2)
		for(City destination : city.neighbors()){
			actions.add(new MDPAction(destination,Type.MOVE));
		}
		
		if(destination != null){
			actions.add(new MDPAction(destination, Type.PICKUP));
		}
		
		return actions;
	}
	
	public String toString(){
		return city + ", " + destination;
	}
}
