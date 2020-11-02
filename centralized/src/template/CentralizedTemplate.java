package template;

import java.io.File;
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
            }/*
            for(Task t: tasks){
                TAction pickup = new TAction(new Pickup(t), t);
                TAction delivery = new TAction(new Delivery(t), t);

                if(first){
                    manager.setFirstAction(vehicles.get(0), pickup);
                    prev = pickup;
                    first = false;
                    manager.setNextAction(pickup, delivery);

                }else{
                    manager.setNextAction(prev, pickup);
                    manager.setNextAction(pickup, delivery);
                    prev = delivery;
                }
            }
            */
            manager.setNextAction(prev, null);

            return new State(manager, vehicles);
    }

    public State SLS(State s){
        State bestState = new State(s);
        State state = bestState.chooseNeighbors();
        int counter = 0;
        
        while(counter < 1000 ){
            System.out.println("Iteration " + counter);
            if(state.cost < bestState.cost){
                bestState = state;
            }
            state = bestState.chooseNeighbors();
            counter++;
            //System.out.println(bestState.cost);
        }
        //System.out.println(bestState.cost);
        return bestState;
    }
    
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        long time_start = System.currentTimeMillis();
        NextActionManager manager;
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        State state = SLS(initialState(vehicles, tasks));
        List<Plan> plans = new ArrayList<Plan>();

        for(Vehicle v: vehicles){
            plans.add(state.toPlan(v));
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
