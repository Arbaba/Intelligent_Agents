package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;
import logist.plan.Action.Move;
import logist.topology.Topology.City;
import logist.plan.Action;
import logist.plan.Plan;

import java.util.List;
import java.util.Random;

class State {
	NextActionManager manager;
	HashMap<Vehicle, List<Integer>> capacityLeft = new HashMap<Vehicle, List<Integer>>();
	HashMap<TAction, Integer> time= new HashMap<TAction, Integer>();
    HashMap<TAction, Vehicle> vehicles  = new HashMap<TAction, Vehicle>();
    HashMap<TAction, TAction> oppositeAction; 
    int cost;
    List<Vehicle> orderedVehicles;
    public State(NextActionManager manager, List<Vehicle> vehicles){
        this.manager = new NextActionManager(manager);
        this.capacityLeft = new HashMap<Vehicle, List<Integer>>();
        this.time = new HashMap<TAction, Integer>();
        this.vehicles = new  HashMap<TAction, Vehicle>();
        this.cost = 0;
        this.oppositeAction = new HashMap<TAction, TAction>();
        this.orderedVehicles = new ArrayList<Vehicle>(vehicles);
        //Initialize time, vehicles and capacityLeft  
        for(Vehicle v : vehicles){
            TAction action = manager.firstPick(v);
            this.capacityLeft.put(v, new ArrayList<Integer>());
            Integer counter = 0;
            while(action != null){
                this.vehicles.put(action, v);
                this.time.put(action, counter);
                action = manager.nextAction(action);
                counter++;
            }
            updateCapacity(v);
        }
        
        //Initialize oppositeAction
        for(TAction a1: time.keySet()){
            for(TAction a2: time.keySet()){
                if(a1 != null && a2 != null && !a1.equals(a2) && a1.task.equals(a2.task)){
                    oppositeAction.put(a1,a2);
                }
            }
        }
    
        //Initialize the cost
        computeCost();
        //System.out.println("Build 0 number of keys" + capacityLeft.keySet().size());
        //System.out.println("Build 0 number of keys" + vehicles.size());
        //System.out.println("Build 0 number of keys" + time.keySet().size());

        checkValidity();
 }
/*    public State(NextActionManager manager,
                 HashMap<Vehicle, List<Integer>> capacity,
                 HashMap<TAction, Integer> time,
                 HashMap<TAction, Vehicle> vehicles, int cost ,
                 List<Vehicle> orderedVehicles
                 ){
        this.manager = new NextActionManager(manager);
        this.capacityLeft = new HashMap<Vehicle, List<Integer>>(capacityLeft);
        this.time = new HashMap<TAction, Integer>(time);
        this.vehicles = new  HashMap<TAction, Vehicle>(vehicles);
        this.time = new HashMap<TAction, Integer>(time); 
        this.cost = cost;
        this.oppositeAction = new HashMap<TAction, TAction>();
        this.orderedVehicles = new ArrayList<Vehicle>(orderedVehicles);
        for(Vehicle v : orderedVehicles){
            this.capacityLeft.put(v, new ArrayList<Integer>());
            TAction action = manager.firstPick(v);
            int counter = 0;
            while(action != null){
                this.time.put(action, counter);
                counter++;
                action = manager.nextAction(action);
            }
            
            updateCapacity(v);
        }
        for(TAction a1: time.keySet()){
            for(TAction a2: time.keySet()){
                if(a1 != null && a2 != null && a1.task.equals(a2.task)){
                    oppositeAction.put(a1,a2);
                }
            }
        }
        System.out.println("Build 1 number of keys" + capacityLeft.keySet().size());
     

    }*/
	
	public State(State other){
        this.manager = new NextActionManager(other.manager);
        this.capacityLeft = new HashMap<Vehicle, List<Integer>>(other.capacityLeft);
        this.time = new HashMap<TAction, Integer>(other.time);
        this.vehicles = new  HashMap<TAction, Vehicle>(other.vehicles);
        this.cost = other.cost;
        this.oppositeAction = new HashMap<TAction, TAction>(other.oppositeAction);
        this.orderedVehicles = new ArrayList<Vehicle>(other.orderedVehicles);
        checkValidity();
	}

    public boolean checkValidity(){
        for(TAction t: oppositeAction.keySet()){
            if(t != null && !(vehicles.get(t) != null) ){
                throw new RuntimeException("Dont find in vehicles t.toString()");
            }
            if(t != null && !(time.get(t) != null) ){
                throw new RuntimeException("Dont find in capacityLeft t.toString()");
            }

            if(t != null && !(vehicles.get(t).equals(vehicles.get(oppositeAction.get(t)))) ){
                throw new RuntimeException("Dont find in vehicles t.toString()");
            }
        }
        return true;
    }

    //Take the first task from the tasks of one vehicle and give it to another vehicle
    public State changeVehicle(Vehicle v1, Vehicle v2){
        System.out.println("Change vehicles");
		State newState = new State(this);
        System.out.println(v1.name() + v2.name());
		//Get v1 first task (pick up)
        TAction aPickUp  = newState.manager.firstPick(v1);
        //Get v1 first task (delivery)
        TAction aDelivery  = oppositeAction.get(aPickUp);

		NextActionManager newManager = newState.manager;

        //Update v1 first task (pick up)
        if(time.get(aDelivery) == 1){
		    newManager.setFirstAction(v1, newManager.nextAction(aDelivery));
        }else {
            newManager.setFirstAction(v1, newManager.nextAction(aPickUp));
        }
        //We update the previous of the removed delivery
        newManager.setNextAction(previousTask(aDelivery), manager.nextAction(aDelivery));
		//Update the next of the popped task
		newManager.setNextAction(aPickUp, aDelivery);
        newManager.setNextAction(aDelivery, manager.firstPick(v2));
        
		//Update the first task of vehicle 2
		newManager.setFirstAction(v2, aPickUp);

		//Update capacity
		newState.updateCapacity(v1);
		newState.updateCapacity(v2);

		//Update time
		newState.updateTime(v1, newManager.firstPick(v1));
		newState.updateTime(v2, newManager.firstPick(v2));
		
        //Update vehicles
        newState.updateVehicles(v2, aPickUp);
        newState.updateVehicles(v2, aDelivery);

        //Compute the cost 
        newState.computeCost();
        checkValidity();
        System.out.println("Change vehicules done");
        
        return newState;
	}
	
    public boolean updateCapacity(Vehicle v){
		int currentCapacity = v.capacity();
        TAction action = manager.firstPick(v);
		List<Integer> capacities = new ArrayList<Integer>();
		int counter = 0;
		while(action != null){
            counter++;
            //System.out.println(action.isPickUp() + action.task.toString());
            if(counter > 1000){
                
                throw new RuntimeException("berk");
            }
            if(action.isPickUp()){
                currentCapacity += action.task.weight;
            }else if(action.isDelivery()){
				currentCapacity -= action.task.weight;
            }
            if(currentCapacity < 0){
                return false;
            }
            capacities.add(currentCapacity);
            action = manager.nextAction(action);
		}
        capacityLeft.put(v, capacities);
        return true;
	}
	
    public void updateTime(Vehicle v, TAction action){
        int counter = 0;
		while(action != null){
            time.put(action, counter);
            action = manager.nextAction(action);
			counter++;
        }
    }

    public void updateVehicles(Vehicle v, TAction action){
        vehicles.put(action, v);
    }

    public TAction previousTask(TAction a){
        TAction next = manager.firstPick(vehicles.get(a));
        TAction previous = null;

        while(next != null && ! next.equals(a)){
            previous = next;
            next = manager.nextAction(next);      
        }
       // if(a.equals(manager.nextAction(previous))) throw new RuntimeException("boo");
        return previous;
    }

    //Change the order of two tasks in the task list of a vehicle
    public State changeTaskOrder(Vehicle v, TAction a1, TAction a2){
        System.out.println("to ");

        State newState = new State(this);
        NextActionManager newManager = newState.manager;
        
        if(newState.time.get(a1) == 0){
            newManager.setFirstAction(v, a2);
        }else if(newState.time.get(a2) == 0){
            newManager.setFirstAction(v, a1);
        }

        //[a1, a2, .........]
        //[....,a2,a1,....]

        //Update a1 previous action null->a2
        newManager.setNextAction(previousTask(a1), a2);
        //Update a2 previous action a1->a1
        if(!previousTask(a2).equals(a1)){
            newManager.setNextAction(previousTask(a2), a1);

            //Update a1 next action
            newManager.setNextAction(a2, manager.nextAction(a1));
            //Update a2 next action
            newManager.setNextAction(a1, manager.nextAction(a2));
        }else{
            newManager.setNextAction(a2, a1);  
            newManager.setNextAction(a1, manager.nextAction(a2));
        }
        

        //Update a1 next action
        //newManager.setNextAction(a2, manager.nextAction(a1));
        //Update a2 next action
        //newManager.setNextAction(a1, manager.nextAction(a2));

        //Update capacity
        newState.updateCapacity(v);

        //Update time
        newState.updateTime(v, newManager.firstPick(v));
        checkValidity();
        System.out.println("to done");
        return newState;    
    }

    public int weightChange(TAction action){
        if(action.isDelivery()){
            return - action.task.weight;
        }else{
            return action.task.weight;
        }
    }

    public boolean isInOrder(TAction action, int expectedTime){
        if(action.isDelivery()){
            return expectedTime > time.get(oppositeAction.get(action));
        }else {
            return expectedTime < time.get(oppositeAction.get(action));
        }
    }
    
    public boolean isSwapValid(TAction left, TAction right){
        /*
        for(TAction t : vehicles.keySet()){
            if(t != null)
            System.out.println(t.task.toString() + t.isPickUp() + " " + t.task.hashCode());
            else
                System.out.println("null action" );
        
            
        }
        
        System.out.println("done");
        System.out.println("check left in v" + vehicles.containsKey(left));
        System.out.println("check left in v" + oppositeAction.containsKey(left));
        System.out.println("check left in v" + oppositeAction.keySet().size());
         
        System.out.println(left.task.toString()+ left.isPickUp() + " "+ left.task.hashCode());
        System.out.println(vehicles.get(left) );
        System.out.println(time.get(left) + " "+ time.size());
        */
		int leftCap = capacityLeft.get(vehicles.get(left)).get(time.get(left));
		int rightCap = capacityLeft.get(vehicles.get(right)).get(time.get(right));

        leftCap -= weightChange(left);
		rightCap -= weightChange(right);

        return (leftCap + weightChange(right) >= 0 && rightCap + weightChange(left) >= 0);
    }

    public void computeCost(){
        TAction a;
        cost = 0;
        for(Vehicle v : orderedVehicles){
            a = manager.firstPick(v);
            City currentCity = v.homeCity();
            while(a != null){
                if(a.isPickUp()){
                    cost += currentCity.distanceTo(a.task.pickupCity) * v.costPerKm();
                    currentCity = a.task.pickupCity;
                }else if(a.isDelivery()){
                    cost += currentCity.distanceTo(a.task.deliveryCity) * v.costPerKm();
                    currentCity = a.task.deliveryCity;
                }
                a = manager.nextAction(a);
            }
        }
    }
    
    public State localChoice(List<State> candidates){
        State best = candidates.get(0);
        for(int i = 1; i < candidates.size(); i++){
            if(best.cost > candidates.get(i).cost){
                best = candidates.get(i);
            }
        }
        return best;
    }
	
    public State chooseNeighbors(){
        
        List<State> candidates= new ArrayList<State>();
        //random vehicle
        Vehicle v = orderedVehicles.get(new Random().nextInt(orderedVehicles.size()));
        System.out.println(orderedVehicles.size());
        //fill with changevehicles
        TAction a = manager.firstPick(v);
        while(a == null){
            v = orderedVehicles.get(new Random().nextInt(orderedVehicles.size()));
            a = manager.firstPick(v);
        }

        for(Vehicle otherVehicle: orderedVehicles){
            if(!v.equals(otherVehicle) /*&& a.task.weight <= otherVehichle.capacity()*/){
                System.out.println("change");
                candidates.add(changeVehicle(v, otherVehicle));
            }
        }
        System.out.println(v.name());
        //fill with changetaskorder
        //System.out.println("number of keys" + capacityLeft.keySet().size());
        
        int length = capacityLeft.get(v).size();
        if(length >=2){
            TAction leftTask = manager.firstPick(v);
            TAction rightTask = manager.nextAction(leftTask);

            for(int t1 = 0; t1 < length - 1; t1++){
                //System.out.println("t1: " + t1 + leftTask);
                for(int t2 = t1 + 1; t2 < length; t2++){
                    //System.out.println("t2: " + t2 + rightTask);
					//if we are not swapping the same task and we don't have weight problems
                    if(isInOrder(leftTask, t2) && isInOrder(rightTask, t1) && isSwapValid(leftTask, rightTask)){
                        State newState = new State(this);
                        newState = newState.changeTaskOrder(v, leftTask, rightTask);
                        newState.computeCost();
                    //    System.out.print("Cost candidate changeTask: " + newState.cost);
                        candidates.add(newState);
                    }
                    rightTask = manager.nextAction(rightTask);
                }
                leftTask = manager.nextAction(leftTask);
                rightTask = manager.nextAction(leftTask);
            }
        }
        System.out.println("choose end");
        return localChoice(candidates);
    }

    public Plan toPlan(Vehicle v){
        List<Action> actions = new ArrayList<Action>();
        TAction action = manager.firstPick(v);
        City currentCity = v.homeCity();
        while(action != null){
            if(action.isPickUp()){
                for (City city : currentCity.pathTo(action.task.pickupCity))
                        actions.add(new Move(city));			//TODO CHECK IF nextAction(aPickUp) == aDelivery
                actions.add(new Pickup(action.task));
                currentCity = action.task.pickupCity;
            }else if(action.isDelivery()){
                for (City city : currentCity.pathTo(action.task.deliveryCity))
                        actions.add(new Move(city));			
                actions.add(new Delivery(action.task));
                currentCity = action.task.deliveryCity;
            }
            action = manager.nextAction(action);
		}
        return new Plan(v.homeCity(), actions);
    }

    public void printPlans(){
        for(Vehicle v: orderedVehicles){
            System.out.println("==== " + v.name() + " ===");
            TAction action = manager.firstPick(v);
            City currentCity = v.homeCity();
            while(action != null){
                if(action.isPickUp()){
                    System.out.println((new Pickup(action.task)));
                    currentCity = action.task.pickupCity;
                }else if(action.isDelivery()){
                    System.out.println(new Delivery(action.task));
                    currentCity = action.task.deliveryCity;
                }
                action = manager.nextAction(action);
		}
        }
    }
}