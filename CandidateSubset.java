import java.util.*;
import java.lang.Math;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

// Each candidateSubset object holds the DCI for each subset size - e.g. 2, 3, 4...
public class CandidateSubset{

	int numHeadings;
	Entropy ent;
	int subSetsSize;
	int subSetLimit;
	int maxSize;
	int probSample;
	double clusterIndex;
	Random rand;
	boolean isRandom;
	List<Agent> birds;
	List<List<Agent>> subsets;
	List<List<Integer>> listProbs;
	List<Integer> subsetValues;
	List<Double> dciValues;
	List<Candidate> candidateSubsets;

	public CandidateSubset(List<Agent> birds, int subSetsSize, int subSetLimit, int numHeadings, int maxSize, int probSample, Random rand, boolean isRandom){
		this.numHeadings = numHeadings;
		this.subSetsSize = subSetsSize;	// Maximum size of a subset
		this.birds = birds;
		this.subSetLimit = subSetLimit;	// Limit on number of subsets to consider
		this.maxSize = maxSize;			// The maximum size of the 'rest of the system' - used to limit calculations for performance gains
		this.probSample = probSample;	// How many heading probabilities to consider per agent
		this.clusterIndex = 0;			// Initialise cluster index value
		this.rand = rand;				// RNG object to use
		this.isRandom = isRandom;		// Flag to indicate the random selector should be used

		ent = new Entropy(numHeadings);	//Initialise Object used to generate entropy values
		subsets = new ArrayList<>();
		dciValues = new ArrayList<>();
		candidateSubsets = new ArrayList<>();
	}

	private void generateSubsets() {
		//TODO: Clean this up - potentially remove the inner getSubSet recursive logic
		for(int subSetIndex = 0; subSetIndex < subSetLimit; subSetIndex++){
			ArrayList<Agent> temp = new ArrayList<Agent>();
			getSubSet(temp, rand.nextInt(birds.size()), subSetsSize);
			subsets.add(temp);
		}
		System.out.println("subsets generated");
	}

	// Calculate the DCI for subsets of this size - work in progress
	public void calculateSubsets(){
		System.out.println("Calculating Subset");
		// Build subsets
		// this.generateSubsets();
		//TODO: USE this.generateSubsets() instead, port isRandom logic
		for(int i = 0; i < subSetLimit; i++){

			List<Agent> temp = new ArrayList<>();
			if(isRandom)
				getSubSet(temp, rand.nextInt(birds.size()), subSetsSize);
			else{
				//System.out.println("We are here " + subSetsSize);
				getSubSet(temp, i, subSetsSize);
			}
			// System.out.println(temp);
			// for(Agent item : temp) {
			// 	System.out.println(item.who);
			// }
			subsets.add(temp);

		}

		System.out.println("Number of subsets to consider: " + subsets.size() + "\n");
		for(int i = 0; i < subsets.size(); i++){
			List<Agent> currentSubset = subsets.get(i);

			// Current subset and rest of the system
			listProbs = new ArrayList<>();
			List<List<Integer>> restOfListProbs = new ArrayList<>();

			/*
				- toList is a method on FlockBird
			*/
			for(int j = 0; j < currentSubset.size(); j++){
				listProbs.add(currentSubset.get(j).toList(probSample));	//Adding list of Freqs
			}

			// This is needed for integration, but is repeated in mutual information, needs a rework (TODO duplicated code, cache in Entropy?)
			ent.anyProbParallel(listProbs);
			//The previous call waits for the pool of Runnables to stop

			double hs = -ent.entropy;	//Flip the sign
			System.out.println("HS of CS: " + hs);	//TODO: Can this be replicated on a paper system?
			
			List<Agent> restOfSystem = new ArrayList<Agent>();

			// Add the rest of the system to the second set, only up to maxSize though
			//TODO: Use this instead - secondList.removeAll(firstList);
			// Why is maxSize being used here? ANS: Used for performance
			
			for(int birdIterator = 0; birdIterator < maxSize; birdIterator++){
				Agent current = birds.get(birdIterator);

				boolean found = false;
				for(int currentSubsetBirdIterator = 0; currentSubsetBirdIterator < currentSubset.size(); currentSubsetBirdIterator++){
					if(currentSubset.get(currentSubsetBirdIterator).who == current.who){
						found = true;
						break;
					}
				}
				if(!found){
					restOfSystem.add(current);
				}
			}

			for(int j = 0; j < restOfSystem.size(); j++){
				restOfListProbs.add(
					restOfSystem.get(j)
					.toList(probSample));		//Gets frequencies out of map (Heading=>Freq)				
			}

			double mutualInfo= ent.mutualInformation(listProbs, restOfListProbs);
			double integrate = ent.integration(currentSubset, hs);

			subsetValues = new ArrayList<>();

			for(int j = 0; j < subsets.get(i).size(); j++){
				subsetValues.add(subsets.get(i).get(j).who);
			}

			clusterIndex = integrate/mutualInfo;
			System.out.println("CLUSTER INDEX:	" + clusterIndex + "\nSubset considered\n");
			candidateSubsets.add(new Candidate(subsetValues, clusterIndex));

			System.out.println("DONE");
		}
	}

	public void getSubSet(List<Agent> subset, int currentPos, int limit){
		subset.add(birds.get(currentPos));
		
		// Getting random subsets could have duplicates - needs work
		if(limit != 1){
			if(isRandom)
				getSubSet(subset, rand.nextInt(birds.size()), limit-1);
			else
				getSubSet(subset, currentPos+1,limit-1);
		}
	}
	public void printSubsets(){
		for(int i = 0; i < candidateSubsets.size(); i++){
			for(int j = 0; j < candidateSubsets.get(i).getAgents().size(); j++){
				System.out.print(candidateSubsets.get(i).getAgents().get(j) + ", ");
			}

			System.out.println("DCI: " + candidateSubsets.get(i).getDci());
			System.out.println();

		}
	}

	public List<Candidate> getCandidates(){
		return candidateSubsets;
	}
}