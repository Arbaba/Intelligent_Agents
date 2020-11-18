package centralized;

import java.io.File;
import java.io.FileWriter;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
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

/*
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    public State initialStateEven(List<Vehicle> vehicles, TaskSet tasks){
            NextActionManager manager = new NextActionManager(vehicles);
            HashMap<TAction, Vehicle> v  = new HashMap<TAction, Vehicle>();
            HashMap<Vehicle, TAction> lastAction = new HashMap<Vehicle,TAction>();
            List<Task> list = new ArrayList<Task>();

            for(Task t: tasks){
                list.add(t);
            }

            for(int i = 0; i< tasks.size(); i++){
                Vehicle currentVehicle =  vehicles.get(i % vehicles.size());
                Task current = list.get(i);

                if(i < vehicles.size()){
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
    public State initialStateClosest(List<Vehicle> vehicles, TaskSet tasks){
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
    public State initialState(List<Vehicle> vehicles, TaskSet tasks){
            NextActionManager manager = new NextActionManager(vehicles);
            HashMap<TAction, Vehicle> v  = new HashMap<TAction, Vehicle>();
            boolean first = true;
            List<Task> list = new ArrayList<Task>();
            for(Task t: tasks){
                list.add(t);
            }
            TAction prev =  new TAction(new Pickup(list.get(0)), list.get(0));
            for(int i = 0; i< tasks.size(); i++){
                Task current = list.get(i);

                if(i==0){
                    TAction pickup = new TAction(new Pickup(list.get(0)), list.get(0));
                    TAction delivery = new TAction(new Delivery(list.get(0)), list.get(0));
    

                    manager.setFirstAction(vehicles.get(0), pickup);
                    manager.setNextAction(pickup, delivery);
                    prev = delivery;
                }else {
                    TAction pickup = new TAction(new Pickup(current), current);
                    TAction delivery = new TAction(new Delivery(current), current);
    
                    manager.setNextAction(prev, pickup);
                    manager.setNextAction(pickup, delivery);
                    prev = delivery;

                }
            }
            manager.setNextAction(prev, null);

            return new State(manager, vehicles);
    }
 
    public boolean outOfTime(long time_start, long maxTime){
        long elapsedTime = System.currentTimeMillis() - time_start;
        long epsilon = 3000;
        boolean stop =  maxTime - elapsedTime < epsilon;
        if(stop){
            System.out.println("Out of time. Best solution found returned");
        }
        return stop;
    }
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        long time_start = System.currentTimeMillis();
        NextActionManager manager;
		//System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        //SLS
        State bestState = initialStateClosest(vehicles, tasks);

        List<State> bests = new ArrayList<State>();

        System.out.println("Initial cost :" + bestState.cost);
        try {
            //FileWriter writer = new FileWriter("p8.txt");
            //writer.write(String.format("%f, %d\n", 0.0f, bestState.cost));

            State state = bestState.chooseNeighbors();

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
                    if(bests.size() > 0 ){
                        state = state.localChoice(bests);  
                    }
                    if(state.cost < bestState.cost){
                        //writer.write(String.format("%d, %d\n", (System.currentTimeMillis() - time_start), bestState.cost));
                        //writer.flush();
                    }
                    bestState = state;

                    bests = new ArrayList<State>();                  
                }else{
                    bests.add(bestState.chooseNeighbors());
                }

                
                if(bestState.cost == state.cost){
                    sameCost++;
                }else{
                    sameCost = 0;
                }
                //state = bestState.chooseNeighbors();
                counter++;
                //System.out.println(bestState.cost);
            }
            
            //bestState.printPlans();
            //writer.flush();
            //writer.close();
        } catch (Exception e) {
            //TODO: handle exception
            System.out.println("An exception occured but we return the best solution found.\n" );
            e.printStackTrace();
        }

        System.out.println("Final cost :" + bestState.cost);

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

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
}
