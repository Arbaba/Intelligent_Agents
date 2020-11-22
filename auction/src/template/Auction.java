package template;
import java.io.File;
import java.lang.reflect.Array;
//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;

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
import logist.simulation.VehicleImpl;
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
	private int numBids;
	private int wonBids;
	private long lastBid;
	ArrayList<Long> pastBids;
	ArrayList<Double> pastCost;
	HashMap<Integer, ArrayList<Task>> tasksPerAgent;
	HashMap<Integer, ArrayList<ArrayList<Vehicle>>> vehiclesPerAgent;
	HashMap<Integer, Long> currentCost;
	HashMap<Integer, Long> computedCost;

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
		this.	numBids = 0;
		currentState = new State(new NextActionManager(agent.vehicles()), agent.vehicles());
		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		this.taskSet = new HashSet<Task>();
		this.pastBids = new ArrayList<Long>();
		this.pastCost = new ArrayList<Double>();
		for(Vehicle v : agent.vehicles()){
			System.out.println(v.name());
		}
		this.vehiclesPerAgent =  new HashMap<Integer,ArrayList<ArrayList<Vehicle>>>();
		this.tasksPerAgent = new HashMap<Integer,ArrayList<Task>>();
		this.currentCost = new HashMap<Integer, Long>();
		this.computedCost = new HashMap<Integer, Long>();


		if(tasksPerAgent.isEmpty()){
			for(int i = 0; i< 2; i++){
				ArrayList<Task> ts = new ArrayList<Task>();
				tasksPerAgent.put(i, ts);
			}
		}

		if(vehiclesPerAgent.isEmpty()){
			//can use topology.randomcity later
			//add to vehicles per agent
			for(int agentIdx = 0; agentIdx <  2; agentIdx++){
				currentCost.put(agentIdx,(long) 0);
				computedCost.put(agentIdx,(long) 0);

				vehiclesPerAgent.put(agentIdx, new ArrayList<ArrayList<Vehicle>>());
				int nSimulations = 3;
				for(int simulationIdx = 0; simulationIdx < nSimulations; simulationIdx++ ){
					vehiclesPerAgent.get(agentIdx).add(new ArrayList<Vehicle>());

					for(int i = 0; i < agent.vehicles().size();  i++){
						Vehicle dummyVehicle = agent.vehicles().get(0);
						City startCity = topology.cities().get((int) Math.floor(Math.random() *topology.cities().size()));
						Vehicle vehicle = (new VehicleImpl(dummyVehicle.id(), dummyVehicle.name(), dummyVehicle.capacity(), dummyVehicle.costPerKm(), startCity, (long) dummyVehicle.speed(), dummyVehicle.color())).getInfo();
						vehiclesPerAgent.get(agentIdx).get(simulationIdx).add(vehicle);
					}
				}
			}	
		}
	}

	/**
	 * Compute the lambda from the exponential distribution according 
	 * to the Maximum likelihood estimation
	 * @param bids
	 * @return
	 */
	public double MLE(ArrayList<Long> bids){	
		double acc = 0;
		int counter = 1;
		for(long bid: bids){
			acc += counter * (bid - pastCost.get(counter));
			counter++;
		}
		return acc / (float) ((bids.size() * (bids.size()+1))/2 );
	}

	/**
	 * 
	 * @param lambda
	 * @return
	 */
	public double sampleExponential(double lambda){
		return Math.log(1 - Math.random()) / (-lambda);
	}

	public long secondLowest(Long[] bids){
		Long[] sorted = Arrays.copyOf(bids, bids.length);
		Arrays.sort(sorted);
		return sorted[1];
	}

	public long meanCost(List<Task> tasks){
		State s = centralized.StateSolution.findBestState(agent.vehicles(), new HashSet<Task>(tasks), timeout_bid);

		return s.getCost();
	}
	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if(tasksPerAgent.isEmpty()){
			for(int i = 0; i< bids.length ; i++){
				ArrayList<Task> ts = new ArrayList<Task>();
				tasksPerAgent.put(i, ts);
			}
		}

		if(vehiclesPerAgent.isEmpty()){
			//can use topology.randomcity later
			//add to vehicles per agent
			for(int agentIdx = 0; agentIdx <  bids.length; agentIdx++){
				vehiclesPerAgent.put(agentIdx, new ArrayList<ArrayList<Vehicle>>());
				int nSimulations = 3;
				for(int simulationIdx = 0; simulationIdx < nSimulations; simulationIdx++ ){
					vehiclesPerAgent.get(agentIdx).add(new ArrayList<Vehicle>());

					for(int i = 0; i < agent.vehicles().size();  i++){
						Vehicle dummyVehicle = agent.vehicles().get(0);
						City startCity = topology.cities().get((int) Math.floor(Math.random() *topology.cities().size()));
						Vehicle vehicle = (new VehicleImpl(dummyVehicle.id(), dummyVehicle.name(), dummyVehicle.capacity(), dummyVehicle.costPerKm(), startCity, (long) dummyVehicle.speed(), dummyVehicle.color())).getInfo();
						vehiclesPerAgent.get(agentIdx).get(simulationIdx).add(vehicle);
					}
				}
			}	
		}
		if(tasksPerAgent.containsKey(winner)){
			tasksPerAgent.get(winner).add(previous);
		}else {
			System.out.println("Key not found " + winner);
		}


		if (winner == agent.id()) {
			taskSet.add(previous);
			currentCity = previous.deliveryCity;
			wonBids++;
			pastBids.add(secondLowest(bids));
		}else{
			pastBids.add(bids[winner]);
			//add task to tasksPerAgent[winner]
		}
		currentCost.put(winner, computedCost.get(winner));
		numBids++;
		for(int i = 0; i < bids.length; i++){
			if(i != agent.id()){
				System.out.println("Opponent bid: " + bids[i]);
			}
		}
		System.out.println("My bid: " + lastBid + " Bid who won:" + bids[winner]);
		System.out.println("Won " + wonBids / (float) numBids * 100 + " % of bids");
		
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
		

		if(tasksPerAgent.isEmpty()){
			long marginalCost = ((newState.getCost() - (currentState.getCost())));
			return marginalCost;
		}else{
			int bestAgentIdx = 0;
			double maxSize = 0;
			//find the best agent (most tasks)
			for(Map.Entry<Integer,ArrayList<Task>> entry : tasksPerAgent.entrySet()){
				int agentId = entry.getKey();
				if(agentId != agent.id()){
					ArrayList<Task> tasks = entry.getValue();
					if(tasks.size() >= maxSize){
						bestAgentIdx = agentId;
						maxSize = tasks.size();
					}
				}
			}

			//compute its average cost with the new task
			long opponentCost = 0;
			long timeForOpponents = timeout_bid / 4;
			ArrayList<Task> opponentTasks = new ArrayList<Task>(tasksPerAgent.get(bestAgentIdx));
			opponentTasks.add(task);
			for(ArrayList<Vehicle> vs: vehiclesPerAgent.get(bestAgentIdx)){
				opponentCost += centralized.StateSolution.findBestState(vs, new HashSet<Task>(opponentTasks), timeForOpponents).getCost() ;
			}
			opponentCost /= vehiclesPerAgent.get(bestAgentIdx).size();
			computedCost.put(bestAgentIdx, opponentCost);

			//Compute the marginal of our agent and adversary agent
			long opponentMarginalCost = opponentCost - currentCost.get(bestAgentIdx);
			double marginalCost = newState.getCost() - currentState.getCost();
			pastCost.add((double) opponentMarginalCost);


			//Taking the task generate profit iven if 
			if(marginalCost <= 0) {
				marginalCost = opponentMarginalCost;
			}
				
			
			double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
			double bid = ratio * marginalCost;
			//System.out.println("dataset size : " + newTaskSet.size());
			lastBid = (long) Math.round(bid * 1.0);
			//return (long) Math.round(bid * 1.0) ;
			
			double lambda = 1 / (double) 100;

			if(pastBids.size() == 0){
				lambda = 1 / (double) 100;
			}else{
				lambda = MLE(pastBids);
			}
			//if their expected bid is higher than ours, sample a bid such that E[sample] = our bid + half the difference 
			/*
			if(opponentMarginalCost > marginalCost ){
				lambda = 1/(marginalCost + (opponentMarginalCost - marginalCost) / 2.0);
			}*/
			
			lastBid = (long)(sampleExponential(2*lambda) + marginalCost);
			//lastBid = (long) marginalCost;
			System.out.println("bid: " + lastBid);
			return lastBid;
			
		}
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
