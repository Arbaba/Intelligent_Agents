package template;

import java.util.HashMap;
import logist.agent.Vehicle;

class State {
	NextActionManager manager;
	HashMap<Vehicle, Integer> capacity = new HashMap<Vehicle, Integer>();
	HashMap<Action, Integer> time= new HashMap<Action, Integer>();
	HashMap<Action, Vehicle> vehicles  = new HashMap<Action, Vehicle>();
	int cost;

    public State(NextActionManager manager,
                 HashMap<Vehicle, Integer> capacity,
                 HashMap<Action, Integer> time,
                 HashMap<Action, Vehicle> vehicles, int cost ){
        this.manager = new NextActionManager(manager);
        this.capacity = new HashMap<Vehicle, Integer>(capacity);
        this.time = new HashMap<Action, Integer>(time);
        this.cost = cost;

	}
	
	public State(State other){
		this.manager = new NextActionManager(other.manager);
        this.capacity = new HashMap<Vehicle, Integer>(other.capacity);
        this.time = new HashMap<Action, Integer>(other.time);
        this.cost = other.cost;

	}

    //Take the first task from the tasks of one vehicle and give it to another vehicle
    public State changeVehicle(State old, Vehicle v1, Vehicle v2){
		State newState = new State(old);
		//Get v1 first task 
		Action a  = newState.manager.FirstPick(v1);
		NextActionManager newManager = newState.manager;

		//Update v1 first task
		newManager.setFirstTask(v1, newManager.nextAction(a));
		//Update the next of the popped task
		newManager.setNextTask(a, newManager.firstTask(v2));
		//Update the first task of vehicle 2
		newManager.setFirstTask(v2, a);

		//Update capacity
		//Update time
		//Update vehicles
    }


    //Change the order of two tasks in the task list of a vehicle
    public State changeTaskOrder(Vehicle v, Action a1, Action a2){

    }


    public State
}