package template;

import java.util.LinkedList;
import java.util.List;

import template.MDPAction.Type;
import logist.topology.Topology.City;

public class State {
	
	public static final int MAX_CITY_ID = 1000;  // maximum city id number
	
	private City src;  // current city
	private City pkg;  // package destination
	
	List<MDPAction> actions;  // possible actions
	
	public State(City c, City d){
		src = c;
		
		// If the task is for the same city it is assumed there is no task at all. 
		pkg = d;
		
		findActions();  // find all possible actions
	}
	
	public City getSrc(){
		return src;
	}
	
	public City getPkgDest(){
		// get the package destination
		return pkg;
	}
	
	@Override
	public int hashCode(){
		// use city id to avoid conflict
		return src.id * MAX_CITY_ID + pkg.id; 
	}
	
	@Override
	public boolean equals(Object o){
		if(!(src.id == (((State) o).src.id))) 
			return false;
		
		return pkg.id == ((State) o).pkg.id;
	}
	
	private void findActions(){
		actions = new LinkedList<MDPAction>();
		
		// Only moves to nearby cities count (because if the city is not a neighbor trajectory is cut in 2)
		for (City dest : src.neighbors())
			actions.add(new MDPAction(dest, Type.MOVE));
		
		// If there is a real task
		if (pkg.id != src.id)
			actions.add(new MDPAction(pkg, Type.PICKUP));
	}
	
	public List<MDPAction> getActions(){
		return actions;
	}
	
	public String toString(){
		return src + ", " + pkg;
	}
}
