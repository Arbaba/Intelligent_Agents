package template;
import java.io.File;

//the list of imports
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.HashMap;

import logist.Measures;
import logist.config.Parsers;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.simulation.VehicleImpl;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;

import logist.topology.Topology;
import logist.topology.Topology.City;

import logist.LogistSettings;
import centralized.TAction;
import centralized.State;
import centralized.NextActionManager;
import centralized.CentralizedTemplate;
import centralized.TAction;
import centralized.StateSolution;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;
/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class Auction implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private State currentState;
	private HashSet<Task> taskSet;
	private long timeout_setup;
	private long timeout_plan;
	private long timeout_bid;
	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
		
		for(City from: topology.cities()){
			for(City to: topology.cities()){
				System.out.println(from.toString() + " -> " + to.toString() + " " + distribution.probability(from, to));
			}
		}
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		currentState = new State(new NextActionManager(agent.vehicles()), agent.vehicles());
		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		this.taskSet = new HashSet<Task>();
		for(Vehicle v : agent.vehicles()){
			System.out.println(v.name());
		}
	}

	/**
	 * Compute the lambda from the exponential distribution according 
	 * to the Maximum likelihood estimation
	 * @param bids
	 * @return
	 */
	public double MLE(long[] bids){	
		double acc = 0;
		for(long bid: bids){
			acc += bid;
		}
		return acc / (float) bids.length;
	}

	/**
	 * 
	 * @param lambda
	 * @return
	 */
	public double sampleExponential(double lambda){
		return Math.log( 1 - Math.random()) / lambda;
	}


	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			taskSet.add(previous);
			currentCity = previous.deliveryCity;
		}
	}
	
	
	@Override
	public Long askPrice(Task task) {
		System.out.println("Bid");
		//careful on timeout!
		State currentState = taskSet.size() == 0 ? new State(new NextActionManager(agent.vehicles()), agent.vehicles()): StateSolution.findBestState(agent.vehicles(), taskSet, timeout_bid);
		HashSet<Task> newTaskSet = new HashSet<Task>(taskSet);
		newTaskSet.add(task);
		State newState =   newTaskSet.size() == 0 ? new State(new NextActionManager(agent.vehicles()), agent.vehicles()):  centralized.StateSolution.findBestState(agent.vehicles(), newTaskSet, timeout_bid);


		/*if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());
		*/
		double marginalCost = ((newState.getCost() - (currentState.getCost())));
		if(marginalCost <= 0) return null;
		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;
		System.out.println("dataset size : " + newTaskSet.size());
		System.out.println(bid);
		return (long) Math.round(bid * 1.5) ;
	}

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("Build plan");
		System.out.println(tasks.size());
        long time_start = System.currentTimeMillis();
        return centralized.StateSolution.plan(vehicles, tasks, timeout_plan);
    }
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
}
