/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */


import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
    private int worldX, worldY;
    private Object2DGrid space;
    private Object2DGrid agentSpace;

    public RabbitsGrassSimulationSpace(int worldX, int worldY){
        this.worldX = worldX;
        this.worldY = worldY;
        this.space = new Object2DGrid(worldX, worldY);
        for(int i = 0; i < worldX; i++){
            for(int j = 0; j < worldY; j++){
                space.putObjectAt(i, j, new Integer(0));
            }
        }
        agentSpace = new Object2DGrid(worldX, worldY);
    }
    public Object2DGrid getCurrentAgentSpace(){
        return agentSpace;
      }
    public int getDim(){
        return this.worldX;
    }
    public void spreadGrass(int money){
        // Randomly place money in moneySpace
        for(int i = 0; i < money; i++){
    
          // Choose coordinates
          int x = (int)(Math.random()*(space.getSizeX()));
          int y = (int)(Math.random()*(space.getSizeY()));
    
          // Get the value of the object at those coordinates
          int currentValue = getMoneyAt(x, y);
          // Replace the Integer object with another one with the new value
          space.putObjectAt(x,y,new Integer(currentValue + 1));
        }
      }
    
      public int getMoneyAt(int x, int y){
        int i;
        if(space.getObjectAt(x,y)!= null){
          i = ((Integer)space.getObjectAt(x,y)).intValue();
        }
        else{
          i = 0;
        }
        return i;
      }

    public Object2DGrid getSpace(){
        return space;
    }

    public boolean isCellOccupied(int x, int y){
        boolean retVal = false;
        if(space.getObjectAt(x, y)!=null) retVal = true;
        return retVal;
      }



      public boolean addAgent(RabbitsGrassSimulationAgent agent){
        boolean retVal = false;
        int count = 0;
        int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();
    
        while((retVal==false) && (count < countLimit)){
          int x = (int)(Math.random()*(agentSpace.getSizeX()));
          int y = (int)(Math.random()*(agentSpace.getSizeY()));
          if(isCellOccupied(x,y) == false){
            agentSpace.putObjectAt(x,y,agent);
            
            agent.setXY(x,y);
            retVal = true;
          }
          count++;
        }
    
        return retVal;
      }
}
