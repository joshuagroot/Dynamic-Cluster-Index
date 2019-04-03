import java.util.*;
import java.lang.Math;

public class FlockBird{

	public int who;
	public ArrayList<Integer> headings;
	public HashMap<Integer, Integer> headingsProb;
	public ArrayList<Integer> probList; 
	public double xcor;
	public double ycor;
	public ArrayList<Integer> flockMates;
	public int nearestNeighbour;
	public double entropy;
	public int numHeadings;
	public double returnEnt;
	public int discretize;


	public FlockBird(int who, double xcor, double ycor, int nearestNeighbour, int discretize){
		this.who = who;
		this.xcor = xcor;
		this.ycor = ycor;
		this.nearestNeighbour = nearestNeighbour;
		this.entropy = 0;
		this.returnEnt = 0;
		this.discretize = discretize;

		flockMates = new ArrayList<Integer>();
		headings = new ArrayList<Integer>();
		headingsProb = new HashMap<Integer, Integer>();
		probList = new ArrayList<Integer>();
	}

	public void addFlockMates(String[] stringFlock){
		for (int i = 1; i < stringFlock.length; i++){
			if (i == stringFlock.length-1){
				stringFlock[i] = stringFlock[i].replace("}", "");
			}
			//System.out.println(IntegerstringFlock[i]);

			flockMates.add(Integer.parseInt(stringFlock[i]));
		}
	}

	public void addHeading(double heading){
		numHeadings++;

		int temp = (int)heading;
		if (headingsProb.containsKey(temp)){
			headingsProb.replace(temp, headingsProb.get(temp) + 1);
		} else{
			addDiscretizedHeading(temp);
		}
		
		headings.add(temp);
	}

	// Add a heading within the range of discretized
	public boolean addDiscretizedHeading(int heading){
		for(int i = 0; i < discretize; i++){
			if(headingsProb.containsKey(heading+i)){
				headingsProb.replace(heading+i, headingsProb.get(heading+i) + 1);
				return true;
			} else if (headingsProb.containsKey(heading-i)){
				headingsProb.replace(heading-i, headingsProb.get(heading-i) + 1);
				return true;
			}
		}
		headingsProb.put(heading, 1);
		return false;
	}

	public ArrayList<Integer> getHeadings(){
		return headings;
	}

	public int getNumHeadings(){
		return numHeadings;
	}

	public HashMap<Integer, Integer> getMap(){
		return headingsProb;
	}

	public void calculateEntropy(){
		Iterator i = headingsProb.values().iterator();

		while (i.hasNext()){
			double temp = (int)i.next();

			entropy += ((temp/numHeadings) * (Math.log(temp/numHeadings)/Math.log(2)));
		}

		entropy = -entropy;
	}

	public double getEntropy(){
		// Check if entropy has already been counted - if not then calculate it
		if (entropy == 0){
			calculateEntropy();
		}
		return entropy;
	}

	// Collect probSample amount of headings in List form
	public ArrayList<Integer> toList(int probSample){
		if(probList.size() != 0){
			return probList;
		}
		Iterator i = headingsProb.values().iterator();
		int count = 0;

		while (i.hasNext() && count < probSample){
			count++;
			int temp = (int)i.next();
			probList.add(temp);
		}
		return probList;
	}
	
	// Calculate joint probability between this bird and another bird
	public double jointProbability(HashMap<Integer, Integer> otherBird){

		double returnEnt = 0;

		Iterator i = headingsProb.values().iterator();	

		while (i.hasNext()){
			Iterator j = otherBird.values().iterator();
			double tempFirst = (int)i.next();

			while (j.hasNext()){
				double tempSecond = (int)j.next();

				double p = (tempFirst/numHeadings) * (tempSecond/numHeadings);
				returnEnt += p * (Math.log(p)/Math.log(2));
			}
		}

		return -returnEnt;
	}
}