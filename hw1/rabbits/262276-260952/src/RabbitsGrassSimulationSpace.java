/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */


import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
    private int worldX, worldY;
    private Object2DGrid space;


    public RabbitsGrassSimulationSpace(int worldX, int worldY){
        this.worldX = worldX;
        this.worldY = worldY;
        this.space = new Object2DGrid(worldX, worldY);
        for(int i = 0; i < worldX; i++){
            for(int j = 0; j < worldY; j++){
                space.putObjectAt(i, j, new Integer(0));
            }
        }
    }

    public Object2DGrid getSpace(){
        return space;
    }
}
