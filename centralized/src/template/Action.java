package template;

import logist.task.Task;
import logist.plan.Action;
import logist.plan.Action.Deliver;
import logist.plan.Action.Pickup;

public class Action {
    Task task;
	Boolean pick;
    
    public Action(Deliver deliver, Task t){
        task = t;
        pick = false; 
    }
    public Action(Pickup pickup, Task t){
        pick = true; 
        task = t;
    }
    boolean isPickUp(){
        return pick;
    }
    
    boolean isDelivery(){
        return !pick;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return (task.hashCode() * 33 + pick.hashCode()) * 33;
    }
}