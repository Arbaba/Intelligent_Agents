package template;

import logist.task.Task;
import java.util.HashMap;
import java.util.List;

import logist.simulation.Vehicle;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;

public class NextActionManager {
    private HashMap<TAction, TAction> nextAction;
    private HashMap<Vehicle, TAction>  firstAction;
    
    public NextActionManager(List<Vehicle> vehicles){
        this.nextAction = new HashMap<TAction, TAction>();
        this.firstAction = new HashMap<Vehicle, TAction>();
        for(Vehicle v: vehicles){
            firstAction.put(v, null);
        }
    }

	public NextActionManager(NextActionManager other){
        this.nextAction = new HashMap<TAction,TAction>(other.nextAction);
        this.firstAction = new HashMap<Vehicle, TAction>(other.firstAction);
    }

	public TAction firstPick(Vehicle vk){
		return firstAction.get(vk);
	}
	
	public TAction nextAction(TAction a){
        //return null if last vehicle action
        return nextAction.get(a);
	}

	public void setFirstAction(Vehicle v, TAction a){
        //if(a != null && a.isDelivery()) {throw new IllegalArgumentException("First action can not be a delivery");}
		firstAction.put(v, a);
	}

	public void setNextAction(TAction a1, TAction a2) {
		if(!a1.equals(a2))//throw new IllegalArgumentException();
		nextAction.put(a1, a2);

	}

	public void removeTask(Task t){
		TAction pick = new TAction(new Pickup(t), t);
		TAction deliver = new TAction(new Delivery(t), t);
        
		nextAction.remove(pick);
		nextAction.remove(deliver);
	}
}