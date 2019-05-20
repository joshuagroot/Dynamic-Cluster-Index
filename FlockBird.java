
import java.util.*;
import java.lang.Math;

public class FlockBird extends Agent{
	double xcor;
	double ycor;
	List<Integer> flockMates;
	int nearestNeighbour;
	
	public FlockBird(int who, double xcor, double ycor, int nearestNeighbour, int discretize){
		super(who, discretize);
		this.xcor = xcor;
		this.ycor = ycor;
		this.nearestNeighbour = nearestNeighbour;
		

		flockMates = new ArrayList<Integer>();
	}

	public void addFlockMates(String[] stringFlock){
		for (int i = 1; i < stringFlock.length; i++){
			if (i == stringFlock.length-1){
				stringFlock[i] = stringFlock[i].replace("}", "");
			}

			flockMates.add(Integer.parseInt(stringFlock[i]));
		}
	}
}