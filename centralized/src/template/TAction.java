package template;

import logist.task.Task;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;

public class TAction {
    Task task;
	Boolean pick;
    
    public TAction(Delivery deliver, Task t){
        task = t;
        pick = false; 
    }
    public TAction(Pickup pickup, Task t){
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

    @Override
    public boolean equals(Object that) {
        // TODO Auto-generated method stub
        if (!(that instanceof TAction)) 
        return false;
        TAction t = (TAction) that;
        return t.task.equals(task) && t.pick == pick;
    
    }
}