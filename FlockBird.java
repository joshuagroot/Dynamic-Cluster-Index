
import java.util.*;
import java.lang.Math;

public class FlockBird extends Agent{
	double xcor;
	double ycor;
	List<List<Integer>> flockMates;
	Map<Integer, Integer> flockMateFrequency;
	int nearestNeighbour;
	
	public FlockBird(int who, double xcor, double ycor, int nearestNeighbour, int discretize){
		super(who, discretize);
		this.xcor = xcor;
		this.ycor = ycor;
		this.nearestNeighbour = nearestNeighbour;
		

		flockMates = new ArrayList<>();
		flockMateFrequency = new HashMap<>();
	}

	public void addFlockMates(List<Integer> newMates){
		flockMates.add(newMates);
		for(int i : newMates){

			if(flockMateFrequency.get(i) == null){
				flockMateFrequency.put(i, 0);
			}
			flockMateFrequency.put(i, flockMateFrequency.get(i)+1);
		}
	}
}