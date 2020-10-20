package template;

import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	public class State{
		City currentCity;
		Set<Task> toPick;
		Set<Task> picked;
		double capacityLeft;
		public State(City currentCity, Set<Task> toPick, Set<Task> picked){
			this.currentCity = currentCity;
			this.toPick = new HashSet<Task>(toPick);
			this.picked = new HashSet<Task>(picked);
			capacityLeft = capacity ;
		}
		public State(City currentCity, List<Task> toPick, List<Task> picked){
			this(currentCity, new HashSet<Task>(toPick), new HashSet<Task>(picked));
		}
		public State(City currentCity, List<Task> toPick){
			this(currentCity, new HashSet<Task>(toPick), new HashSet<Task>());
		}
		//removes task from picked
		public State removePickedTask(Task t){
			List<Task> updatedPicked = new ArrayList<Task>(picked);
			updatedPicked.remove(t);
			capacityLeft += t.weight;
			return new State(t.deliveryCity, new ArrayList<Task>(toPick), updatedPicked);
		}
		//moves task from toPick and adds it to picked
		public State pickTask(Task t){
			List<Task> updatedToPick = new ArrayList<Task>(toPick);
			List<Task> updatedPicked = new ArrayList<Task>(picked);
			updatedPicked.add(t);
			capacityLeft -= t.weight;
			updatedToPick.remove(t);
			return new State(t.pickupCity, updatedToPick, updatedPicked);
		} 

		public boolean canPickup(Task t){
			return t.weight <= capacityLeft;
		}

		public State moveTo(City city){
			return new State(city, toPick, picked);
		}
		public boolean isGoal(){
			return (toPick.size()==0 && picked.size()==0);
		}

		@Override
		public boolean equals(Object that){
			if (!(that instanceof State)) 
			return false;
			State thatState = (State) that;
			return this.currentCity.equals(thatState.currentCity)
					&& toPick.size() == thatState.toPick.size()
					&& picked.size() == thatState.picked.size()
					&& toPick.containsAll(thatState.toPick)
					&& picked.containsAll(thatState.picked);
		}
		@Override
		public int hashCode() {
			// TODO Auto-generated method stub
			int hash = 5581;
			hash = 33*hash + currentCity.hashCode();
			hash = 33*hash + toPick.hashCode();
			hash = 33*hash + picked.hashCode();
			return hash;		
		}
		
		@Override
		public String toString() {
			return String.format("State {city: %s, picked: %s, toPick: %s}", currentCity, picked, toPick);
		}

	}
	
	
	public class Node{
		double cost;
		double fcost;
		State state;

		Node closestParent;
		List<Action> parentActions;

		public Node(State state,  Node  closestParent, List<Action>  parentActions ,double cost){
			this.state = state;
			this.closestParent = closestParent;
			this.parentActions = parentActions;
			this.cost = cost;
		}

		@Override
		public String toString() {
			return String.format("Neighbor {state: %s, cost %f}", state, cost);
		}
	}
	
	
	
	public List<Node> computeNeighbors(Node parent, double parentCost ){
		List<Node> neighbors =  new ArrayList<Node>();
		State state = parent.state;
		if(state.isGoal()){
			//System.out.println("no neighbors");
			return neighbors;
		}
		//delivery
		for(Task task: state.picked){
			State newState = state.removePickedTask(task);
			List<Action> actions = new ArrayList<Action>();
			double cost =  state.currentCity.distanceTo(task.deliveryCity) + parentCost;
			for (City city : state.currentCity.pathTo(task.deliveryCity))
				actions.add(new Move(city));
			
			Action action =  new Delivery(task);
			actions.add(action);
			//logger.write(action);
			
			neighbors.add(new Node(newState,parent, actions, cost));
		}
			
		//pickup
		for(Task task: state.toPick){
			if(state.canPickup(task)){
				State newState = state.pickTask(task);
				List<Action> actions = new ArrayList<Action>();
	
				double cost = state.currentCity.distanceTo(task.pickupCity) + parentCost;
				for (City city : state.currentCity.pathTo(task.pickupCity))
						actions.add(new Move(city));			
				Action action = new Pickup(task);
				//logger.write(action);
				actions.add(action);
				neighbors.add(new Node(newState, parent, actions, cost));
			}
			
		}
		return neighbors;
	}


	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	Logger logger ;
	List<Task> carriedTasks;
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		this.carriedTasks = new ArrayList<Task>();
		// initialize the planner
		capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		logger = new Logger();
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		long startTime = 0;
		long endTime = 0;
		System.out.println(algorithm);
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			startTime = System.nanoTime();
			plan = aStarPlan(vehicle, tasks);
			endTime = System.nanoTime();
			break;
		case BFS:
			startTime = System.nanoTime();
			plan = bfsPlan(vehicle, tasks);
			endTime = System.nanoTime();
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		long durationInNano = (endTime - startTime);	
		long durationInMillis = TimeUnit.NANOSECONDS.toMillis(durationInNano);
		System.out.println("Time to compute time: " + durationInMillis + " ms");
		return plan;
	}

	private LinkedList<Node> mergeSortedCollections(LinkedList<Node> xList, LinkedList<Node> yList){
		LinkedList<Node> acc = new LinkedList<Node>();
		int finalSize = xList.size() + yList.size();
		while(acc.size() <  finalSize){
			if(xList.size() > 0 && yList.size() > 0){
			  if(xList.element().fcost <= yList.element().fcost){
				acc.add(xList.pop());
			  }else{
				acc.add(yList.pop());
			  }
			}else if(xList.size() > 0){
			  acc.add(xList.pop());
			}else if(yList.size() > 0){
			  acc.add(yList.pop());
			}
		}
		return acc;
	}
	
	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks){
		System.out.println("Build ASTAR plan");
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		List<Task> toPick = new ArrayList<Task>();
		for(Task t: tasks){
			toPick.add(t);
		}
		State initState = new State(current, toPick, carriedTasks); 
		Node initNode = new Node(initState, null, new ArrayList<Action>(),0);
		LinkedList<Node> Q = new LinkedList<Node>();
		Q.add(initNode);
		HashMap<State, Node> C = new HashMap<State, Node>();		
		Node best = null;
		int counter = 0;
		while(Q.size() != 0){
			//logger.logCounter(counter);
			counter++;
			Node node = Q.pop();
			double fcost = node.fcost;
			double cost = node.cost;
			if(node.state.isGoal()){
				best = node;
				break;
			}


			//If we already visited the node we check if the cost is inferior
			if(C.containsKey(node.state) && C.get(node.state).cost > cost){
				double difference = C.get(node.state).cost-cost;
				C.get(node.state).cost = node.cost;
				C.get(node.state).closestParent = node.closestParent;
				C.get(node.state).parentActions = node.parentActions;
				updateCostsASTAR(C, C.get(node.state), difference);
			}

			//System.out.println("Current City" + neighbor.node.state.currentCity);
			if(!C.containsKey(node.state)){
				//logger.writeQueue(Q, node);
				C.put(node.state, node);//put in list of visited nodes
				//logger.logNode(node);
	
				List<Node> neighbors = computeNeighbors(node,  cost);
				
				for(Node neigh: neighbors){
					neigh.fcost = neigh.cost + hMaxDistance(neigh);
					//neigh.fcost = node.cost + hAvg(neigh, node);
					//neigh.fcost = node.cost + hNearest(neigh);
					//neigh.fcost = neighbor.cost;
				}
				
				if(false){
					for(Node n : neighbors){
						Q.add(n);
					}
					Collections.sort(Q, new NeighborComparator());

				}else {
					//Merge two sorted Lists
					Collections.sort(neighbors, new NeighborComparator());
					Q = mergeSortedCollections(Q, new LinkedList<Node>(neighbors));
				}
				
			}
		}
		//Aggregate actions by backtracking up to the root 
		double bestCost = Double.POSITIVE_INFINITY;
		LinkedList<Action> bestActions = new LinkedList<Action>();
		while(!best.equals(initNode)){
			for (int i = best.parentActions.size() - 1; i >= 0; i--) {
				bestActions.addFirst(best.parentActions.get(i));
			}
			best = best.closestParent;
		}
		logger.write("Done");
		return new Plan(initNode.state.currentCity, bestActions);

	}
	public  static class Logger{
		
		//FileWriter writer;
		int writerCount;
		public Logger(){
			writerCount = 0;
			try {
				
				//writer = new FileWriter("log.txt");
			} catch (Exception e) {
				//TODO: handle exception
			}
		}
		
		public  void logCounter(int counter){
			if(counter % 1000 == 0 || true ) {
				write("Counter: " +  counter);
			}
		}
		public  void logNode(Node n){
			//write(n.toString());
		}

		public void write(String str){
			writerCount++;
			write((Object) str);
		}

		public void writeQueue(LinkedList<Node> queue, Node node){
			StringBuilder b = new StringBuilder(String.format("Queue (current node %s): \n", node.state.currentCity));
			for(Node n: queue){
				Action a = null;
				/*for(Action aprime: n.plan){
					a = aprime;
				}*/
				//b.append(String.format("   city: %s, cost: %f, lastAction: %s\n", n.node.state.currentCity, n.cost, a));
			}
			//write(b.toString());
		}

		public void write(Object o){
			writerCount++;
			//writeToFile(o.toString());
			//writeToConsole(o.toString());
		}
		public void writeToFile(String str){
			try {
				
				//writer.write(str + "\n");
					//writer.flush();
				
			} catch (Exception e) {
				//TODO: handle exception
			}
		}
		public void writeToConsole(String str){
			System.out.println(str);
		}
	}
	class NeighborComparator implements Comparator<Node> 
	{ 
		public int compare(Node a, Node b) 
		{ 
			if (a.fcost < b.fcost)
  				return -1;
			else if (a.fcost > b.fcost)
  				return 1;
			else
  				return 0;
		} 
	}

	
	private double hAverage(Node n, Node parent){
		double avgX = 0;
		double avgY = 0;
		double size = parent.state.picked.size() + parent.state.toPick.size();
		for(Task t: parent.state.picked){
			avgX += t.deliveryCity.xPos;
			avgY += t.deliveryCity.yPos;
		}
		for(Task t: parent.state.toPick){
			avgX += t.pickupCity.xPos;
			avgY += t.pickupCity.yPos;
		}
		avgX /= size;
		avgY /= size;

		return Math.sqrt(Math.pow(n.state.currentCity.xPos - avgX, 2) + Math.pow(n.state.currentCity.yPos - avgY, 2));
	}
	
	private double hMaxDistance(Node n){
		double maxDist = 0;
		double dist;

		for(Task t : n.state.toPick){
			if(n.state.canPickup(t)){
				dist = n.state.currentCity.distanceTo(t.pickupCity);
				if(dist > maxDist){
					maxDist = dist;
				}
			}
		}

		for(Task t : n.state.picked){
			dist = n.state.currentCity.distanceTo(t.deliveryCity);
			if(dist > maxDist){
				maxDist = dist;
			}
		}

		return maxDist;
	}

	private double hNearest(Node n){
		Task toRemovePicked = null;
		Task toRemoveToPick = null;
		double cost = 0.0;
		HashSet<Task> toPick = new HashSet<Task>(n.state.toPick);
		HashSet<Task> picked = new HashSet<Task>(n.state.picked);
		while(toPick.size() + picked.size() > 0){
			double costToPick = Double.POSITIVE_INFINITY;
			double costPicked = Double.POSITIVE_INFINITY;
			for(Task t: picked){
				double dist = n.state.currentCity.distanceTo(t.deliveryCity);
				if(dist < costPicked){
					costPicked = dist;
					toRemovePicked = t;
				}
			}
			for(Task t: toPick){
				double dist = n.state.currentCity.distanceTo(t.pickupCity);
				if(dist < costToPick){
					costToPick = dist;
					toRemoveToPick = t;
				}
			}

			if(costToPick < costPicked){
				toPick.remove(toRemoveToPick);
				cost += costToPick;
			}else {
				picked.remove(toRemovePicked);
				cost += costPicked;
			}
		}
		
		return cost;
	}


	private void updateCosts(Collection<Node> Q, Node parent){
			for(Node possibleChild: Q){
				if(possibleChild.closestParent != null && possibleChild.closestParent.state.equals(parent.state)){
					possibleChild.cost = parent.cost + possibleChild.state.currentCity.distanceTo(parent.state.currentCity);
				}
			}
	}

	private void updateCostsASTAR(HashMap<State, Node> C, Node parent, double difference){
			for(Node possibleChild : C.values()){
				if(possibleChild.closestParent != null && possibleChild.closestParent.state.equals(parent.state)){
					possibleChild.fcost -= difference;
				}
			}
	}

	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks){
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		List<Task> toPick = new ArrayList<Task>();
		for(Task t: tasks){
			toPick.add(t);
		}
		State initState = new State(current, toPick, carriedTasks);

		LinkedList<Node> Q = new LinkedList<Node>();
		Node initNode = new Node(initState, null, new ArrayList<Action>(),0);
		Q.add(initNode);
		HashMap<State, Node> C = new HashMap<State, Node>();		
		int counter = 0;

		while(Q.size() != 0){
			//logger.logCounter(counter);
			counter++;
			Node node = Q.pop();
			double cost = node.cost;
			if(node.state.isGoal()){
				//logger.write("Goal reached");
			}
			//If we already visited the node we check if the cost is inferior
			if(C.containsKey(node.state) && C.get(node.state).cost > cost){
				C.get(node.state).cost = node.cost;
				C.get(node.state).closestParent = node.closestParent;
				C.get(node.state).parentActions = node.parentActions;
				updateCosts(Q, C.get(node.state));
			}

			if(!C.containsKey(node.state)){
				//logger.writeQueue(Q, node);

				C.put(node.state, node);//put in list of visited nodes
				////logger.logNode(node);
				List<Node> neighbors = computeNeighbors(node,  cost);
				for(Node n : neighbors){
					Q.add(n);
				}
			}
		}

		Node best = null; 
		double bestCost = Double.POSITIVE_INFINITY;

		//Find the best goal
		for(Node neighbor : C.values()){
			if(neighbor.state.isGoal() && neighbor.cost < bestCost){
				best = neighbor;
				bestCost = neighbor.cost;
			}
		}

		//Aggregate actions
		LinkedList<Action> bestActions = new LinkedList<Action>();
		while(!best.equals(initNode)){
			for (int i = best.parentActions.size() - 1; i >= 0; i--) {
				bestActions.addFirst(best.parentActions.get(i));
			}
			best = best.closestParent;
		}
		logger.write("Done");
		return new Plan(initNode.state.currentCity, bestActions);

	}
	 Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
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

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
			this.carriedTasks = new ArrayList<Task>(carriedTasks);
		}
	}
}
