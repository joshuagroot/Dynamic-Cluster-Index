import java.util.*;

public class Agent{

	// the id of the Agent
	int who;
	// All the headings of the agent during its run
	List<Integer> headings;
	// The probabilities of each heading - number of occurences
	Map<Integer, Integer> headingsProb;

	// The probabilities of each heading in list form
	List<Integer> probList; 
	int numHeadings;

	// The range which we consider two headings to be the same heading
	int discretize;

	// Entropy value for this agent
	double entropy;

	public Agent(int who, int discretize){
		this.who = who;
		this.discretize = discretize;
		this.entropy = 0;

		headings = new ArrayList<Integer>();
		headingsProb = new HashMap<Integer, Integer>();
		probList = new ArrayList<Integer>();
	}

	// Add a new heading to the agents heading map. If the heading already exists, increment its count
	public void addHeading(double heading){
		numHeadings++;

		int intHeading = (int)heading;
		if (headingsProb.containsKey(intHeading)){
			headingsProb.replace(intHeading, headingsProb.get(intHeading) + 1);
		} else{
			addDiscretizedHeading(intHeading);
		}
		
		headings.add(intHeading);
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

	// Renove probSample amound of heading counts from the headings map and return them as a list
	public List<Integer> toList(int probSample){
		if(probList.size() != 0){
			return probList;
		}
		Iterator<Integer> i = headingsProb.values().iterator();
		int count = 0;

		while (i.hasNext() && count < probSample){
			count++;
			int temp = i.next();
			probList.add(temp);
		}
		return probList;
	}

	public void calculateEntropy(){
		Iterator<Integer> i = headingsProb.values().iterator();

		while (i.hasNext()){
			double temp = (long)i.next();

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

	public List<Integer> getHeadings(){
		return headings;
	}

	public int getNumHeadings(){
		return numHeadings;
	}

	public Map<Integer, Integer> getMap(){
		return headingsProb;
	}

}