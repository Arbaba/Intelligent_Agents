package centralized;

import java.io.File;
import java.io.FileWriter;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.AbstractSet;

import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;

import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class StateSolution {
    public static State initialStateClosest(List<Vehicle> vehicles, AbstractSet<Task> tasks){
        NextActionManager manager = new NextActionManager(vehicles);
        HashMap<TAction, Vehicle> v  = new HashMap<TAction, Vehicle>();
        HashMap<Vehicle, TAction> lastAction = new HashMap<Vehicle,TAction>();
        List<Task> list = new ArrayList<Task>();

        for(Task t: tasks){
            list.add(t);
        }

        for(int i = 0; i< tasks.size(); i++){
            Task current = list.get(i);

            int closestVehicleIdx = 0;
            double closestDistance = Double.POSITIVE_INFINITY;
            for(int idx =0; idx < vehicles.size(); idx++ ){
                City city = vehicles.get(idx).homeCity();
                if(lastAction.get(vehicles.get(idx)) != null){
                    city = lastAction.get(vehicles.get(idx)).targetCity();
                }
                double distance = city.distanceTo(current.pickupCity);
                if(distance <closestDistance){
                    closestVehicleIdx = idx;
                    closestDistance = distance;
                }
            }
            Vehicle currentVehicle =  vehicles.get(closestVehicleIdx);

            if(lastAction.get(currentVehicle) == null){
                TAction pickup = new TAction(new Pickup(current), current);
                TAction delivery = new TAction(new Delivery(current), current);

                manager.setFirstAction(currentVehicle, pickup);
                manager.setNextAction(pickup, delivery);
                lastAction.put(currentVehicle, delivery);
            }else {                    
                TAction pickup = new TAction(new Pickup(current), current);
                TAction delivery = new TAction(new Delivery(current), current);

                manager.setNextAction(lastAction.get(currentVehicle), pickup);
                manager.setNextAction(pickup, delivery);
                lastAction.put(currentVehicle, delivery);
            }
        }

        for(Vehicle car: vehicles){
            manager.setNextAction(lastAction.get(car), null);
        }

        return new State(manager, vehicles);
    }

    public static boolean outOfTime(long time_start, long maxTime){
        long elapsedTime = System.currentTimeMillis() - time_start;
        long epsilon = 3000;
        boolean stop =  maxTime - elapsedTime < epsilon;
        if(stop){
            System.out.println("Out of time. Best solution found returned");
        }
        return stop;
    }

    public static State findBestState(List<Vehicle> vehicles, AbstractSet<Task> tasks, long timeout_plan){
        long time_start = System.currentTimeMillis();
        NextActionManager manager;
		//System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        //SLS
        State state = initialStateClosest(vehicles, tasks);
        int bestCost = state.cost;

        List<State> candidates = new ArrayList<State>();

        System.out.println("Initial cost :" + state.cost);
        
        try {
            //FileWriter writer = new FileWriter("p8.txt");
            //writer.write(String.format("%f, %d\n", 0.0f, bestState.cost));

            //State state = bestState.chooseNeighbors();

            int counter = 0;
            Random rng = new Random(5);
            int sameCost = 0;
            while(!outOfTime(time_start, timeout_plan)){ 
                //if p< rndm
                //compute new best and put it in the list
                //else
                //take the best form the list
                //reset the list
                if((rng.nextFloat()) < 0.2){
                    if(candidates.size() > 0 ){
                        State temp = state.localChoice(candidates);  
                        if(temp.cost < state.cost){
                            state = temp;
                        } 
                    }
                    candidates = new ArrayList<State>();                  
                }else{
                    candidates.add(state.chooseNeighbors());
                }

                if(bestCost == state.cost){
                    sameCost++;
                }else{
                    sameCost = 0;
                }
                if(sameCost == 1000){
                    break;
                }
                //state = bestState.chooseNeighbors();
                counter++;
                //System.out.println(state.cost);
            }
            
            //bestState.printPlans();
            //writer.flush();
            //writer.close();
        } catch (Exception e) {
            //TODO: handle exception
            System.out.println("An exception occured but we return the best solution found.\n" );
            e.printStackTrace();
        }

        System.out.println("Final cost :" + state.cost);
        return state;
    }

    public static List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks, long timeout_plan) {
        long time_start = System.currentTimeMillis();

        State bestState = findBestState(vehicles, tasks, timeout_plan);

        //END SLS
        List<Plan> plans = new ArrayList<Plan>();

        for(Vehicle v: vehicles){
            plans.add(bestState.toPlan(v));
        }
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
    }
}