package template;

import logist.topology.Topology.City;

public class MDPAction{
	
	private City dest;
	private Type t;
	
    public enum Type{
        PICKUP(0), MOVE(1);
        
        private final int value;
        private Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
	
	public MDPAction(City c, Type t){
		this.dest = c;
		this.t = t;
	}
	
	public City getDest(){
		return dest;
	}
	
	public Type getType(){
		return t;
	}
	
	@Override
	public int hashCode(){
		// use id to avoid conflict
		return dest.id * State.MAX_CITY_ID + t.getValue();
	}
	
	@Override
	public boolean equals(Object o){
		if (!t.equals(((MDPAction) o).t)) 
			return false;
		if(dest.id != ((MDPAction) o).dest.id) 
			return false;
		return true;
	}
	
	public String toString(){
		return t + ", " + dest;
	}
	
}
