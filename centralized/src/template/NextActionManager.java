package template;

import logist.task.Task;
import java.util.HashMap;
import logist.simulation.Vehicle;
import logist.plan.Action.Deliver;
import logist.plan.Action.Pickup;

public class NextActionManager {
    private HashMap<Action, Action> nextAction;
	private HashMap<Vehicle, Action>  firstAction;

    public NextActionManager(HashMap<Action, Action> nextAction, HashMap<Vehicle, Action>  firstAction ){
        this.nextAction = new HashMap<Action, Action>(nextAction);
        this.firstAction = new HashMap<Vehicle, Action>(firstAction);
	}
	
	public NextActionManager(NextActionManager other){
        this.nextAction = new HashMap<Action, Action>(other.nextAction);
        this.firstAction = new HashMap<Vehicle, Action>(other.firstAction);
    }

	public Action firstAction(Vehicle vk){
		return firstAction.get(vk);
	}
	
	public Action nextAction(Action a){
        //return null if last vehicle action
        return nextAction.get(a);
	}

	public void setFirstAction(Vehicle v, Action a){
		if(a.isDelivery()) {throw new IllegalArgumentException("First action can not be a delivery");}
		firstAction.put(v, a);
	}

	public void setNextAction(Action a1, Action a2) {
		nextAction.put(a1, a2);
	}

	public void removeTask(Task a){
		Action pick = new Action(new Pickup(a), a);
		Action deliver = new Action(new Deliver(a), a);
        
		nextAction.remove(pick);
		nextAction.remove(deliver);
	}

	public void addTask(Task t, int i1, int i2){

	}
}