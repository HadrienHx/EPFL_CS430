package template;

import logist.topology.Topology.City;

public class MDPAction{
	private City destination;
	private Type type;
	
    public enum Type{
        PICKUP,
        MOVE
    }
	
	public MDPAction(City c, Type t){
		destination = c;
		type = t;
	}
	
	public City getDestination(){
		return destination;
	}
	
	public Type getType(){
		return type;
	}
	
	@Override
	public int hashCode(){
		return destination.hashCode() + 11*type.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if (!type.equals(((MDPAction) o).type)) return false;
		if(!destination.equals(((MDPAction) o).destination)) return false;
		
		return true;
	}
	
	public String toString(){
		return type + ", " + destination;
	}
	
}
