/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */


import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
    private int worldX, worldY;
    private Object2DGrid grassSpace;
    private Object2DGrid agentSpace;

    public RabbitsGrassSimulationSpace(int worldX, int worldY){
        this.worldX = worldX;
        this.worldY = worldY;
        this.grassSpace = new Object2DGrid(worldX, worldY);
        for(int i = 0; i < worldX; i++){
            for(int j = 0; j < worldY; j++){
              grassSpace.putObjectAt(i, j, new Integer(0));
            }
        }
        agentSpace = new Object2DGrid(worldX, worldY);
    }
    public Object2DGrid getCurrentAgentSpace(){
        return agentSpace;
      }
    public int getDimX(){
        return this.worldX;
    }

    public int getDimY(){
      return this.worldY;
    }

    public void spreadGrass(int grass, int maxGrassGrowth){
        // Randomly place grass in grassSpace
        int countLimit = 10 * grassSpace.getSizeX() * grassSpace.getSizeY();
        int count = 0;
        int grassPlaced = 0;

        while((count < countLimit) && (grassPlaced < grass)){
          // Choose coordinates
          int x = (int)(Math.random()*(grassSpace.getSizeX()));
          int y = (int)(Math.random()*(grassSpace.getSizeY()));

          // Get the value of the object at those coordinates
          int currentValue = getGrassAt(x, y);

          // Replace the Integer object with another one with the new value
          if(currentValue < maxGrassGrowth){
            grassSpace.putObjectAt(x,y,new Integer(currentValue + 1));
            grassPlaced++;
          }
          count++;
        }

      }
    
      public int getGrassAt(int x, int y){
        int i;
        if(grassSpace.getObjectAt(x,y)!= null){
          i = ((Integer)grassSpace.getObjectAt(x,y)).intValue();
        }
        else{
          i = 0;
        }
        return i;
      }

    public Object2DGrid getGrassSpace(){
        return grassSpace;
    }

    public boolean isCellOccupied(int x, int y){
        boolean retVal = false;
        if(agentSpace.getObjectAt(x, y)!=null) retVal = true;
        return retVal;
      }



      public boolean addAgent(RabbitsGrassSimulationAgent agent){
        agent.setRabbitGrassSpace(this);
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
      public void removeAgentAt(int x, int y){
        agentSpace.putObjectAt(x, y, null);
      }

      
      public boolean moveAgentAt(int x, int y, int newX, int newY){
        boolean retVal = false;
        if(!isCellOccupied(newX, newY)){
          RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)agentSpace.getObjectAt(x, y);
          if(cda != null){
            removeAgentAt(x,y);
            cda.setXY(newX, newY);
            agentSpace.putObjectAt(newX, newY, cda);
            retVal = true;
          }
        }
        return retVal;
      }
      public int eatGrassAt(int x, int y){
        int grassEnergy = getGrassAt(x, y);
        grassSpace.putObjectAt(x, y, new Integer(0));
        return grassEnergy;
      }

      public int getTotalGrass(){
        int total = 0;
        for(int i = 0; i < agentSpace.getSizeX(); i++){
          for(int j = 0; j < agentSpace.getSizeY(); j++){
            total += ((Integer) grassSpace.getObjectAt(i, j)).intValue();
          }
        }
        return total;
      }
}
