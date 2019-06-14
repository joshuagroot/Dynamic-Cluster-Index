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
	List<Agent> agents;
	List<List<Agent>> subsets;
	List<List<Integer>> listProbs;
	List<Integer> subsetValues;
	List<Double> dciValues;
	List<Candidate> candidateSubsets;

	public CandidateSubset(List<Agent> agents, int subSetsSize, int subSetLimit, int numHeadings, 
		int maxSize, int probSample, int numThreads, Random rand, boolean isRandom){

		this.numHeadings = numHeadings;
		this.subSetsSize = subSetsSize;	// Maximum size of a subset
		this.agents = agents;

		this.subSetLimit = subSetLimit;	// Limit on number of subsets to consider
		this.maxSize = maxSize;			// The maximum size of the 'rest of the system' - used to limit calculations for performance gains
		this.probSample = probSample;	// How many heading probabilities to consider per agent
		this.clusterIndex = 0;			// Initialise cluster index value
		this.rand = rand;				// RNG object to use
		this.isRandom = isRandom;		// Flag to indicate the random selector should be used

		ent = new Entropy(numHeadings, numThreads);	//Initialise Object used to generate entropy values

		subsets = new ArrayList<>();
		dciValues = new ArrayList<>();
		candidateSubsets = new ArrayList<>();
	}

	private void generateSubsets() {
		//TODO: Clean this up - potentially remove the inner getSubSet recursive logic
		for(int subSetIndex = 0; subSetIndex < subSetLimit; subSetIndex++){
			ArrayList<Agent> temp = new ArrayList<>();
			getSubSet(temp, rand.nextInt(agents.size()), subSetsSize);
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
				getSubSet(temp, rand.nextInt(agents.size()), subSetsSize);
			else{
				//System.out.println("We are here " + subSetsSize);
				getSubSet(temp, i, subSetsSize);
			}
			subsets.add(temp);
		}
	}
	public void calculateDCI(){	
		System.out.println("Number of subsets to consider: " + subsets.size() + "\n");
		for(int i = 0; i < subsets.size(); i++){
			List<Agent> currentSubset = subsets.get(i);

			// Current subset and rest of the system
			listProbs = new ArrayList<>();
			List<List<Integer>> restOfListProbs = new ArrayList<>();

			/*
				- toList is a method on  FlockBird
			*/
			for(int j = 0; j < currentSubset.size(); j++){
				listProbs.add(currentSubset.get(j).toList(probSample));
			}
			double startTime = System.currentTimeMillis();

			// This is needed for integration, but is repeated in mutual information, needs a rework (TODO duplicated code, cache in Entropy?)
			ent.anyProbParallel(listProbs);
			//The previous call waits for the pool of Runnables to stop

			double hs = -ent.entropy;	//Flip the sign
			System.out.println("HS of CS: " + hs);	//TODO: Can this be replicated on a paper system?
			
			List<Agent> restOfSystem = new ArrayList<>();

			// Add the rest of the system to the second set, only up to maxSize though
			//TODO: Use this instead - secondList.removeAll(firstList);
			// Why is maxSize being used here? ANS: Used for performance
			int tempMax = maxSize;
			int count = 0;
			System.out.println("Number of agents: " + agents.size());

			while(count != maxSize){

				Agent current = agents.get(rand.nextInt(agents.size()));

				if(!restOfSystem.contains(current) && !currentSubset.contains(current)){
					restOfSystem.add(current);
					count++;
				}
			}

			System.out.println("CURRENT SET: " + currentSubset);
			System.out.println("REST OF SYSTEM: " + restOfSystem);
			// for(int agentIterator = 0; agentIterator < tempMax; agentIterator++){
			// 	Agent current = agents.get(agentIterator);

			// 	boolean found = false;
			// 	for(int currentSubsetAgentIterator = 0; currentSubsetAgentIterator < currentSubset.size(); currentSubsetAgentIterator++){
			// 		if(currentSubset.get(currentSubsetAgentIterator).who == current.who){
			// 			found = true;
			// 			tempMax++;
			// 			break;
			// 		}
			// 	}
			// 	if(!found){
			// 		restOfSystem.add(current);
			// 	}
			// }
			//System.out.println ("rest of system: " + restOfSystem);

			for(int j = 0; j < restOfSystem.size(); j++){
				restOfListProbs.add(restOfSystem.get(j).toList(probSample));
			}


			double mutualInfo= ent.mutualInformation(listProbs, restOfListProbs);
			double integrate = ent.integration(currentSubset, hs);

			subsetValues = new ArrayList<>();

			for(int j = 0; j < subsets.get(i).size(); j++){
				subsetValues.add(subsets.get(i).get(j).who);
			}

			clusterIndex = integrate/mutualInfo;

			double endTime = System.currentTimeMillis();

			System.out.println("CLUSTER INDEX:	" + clusterIndex + "\nSubset considered\n");
			candidateSubsets.add(new Candidate(subsetValues, clusterIndex, endTime-startTime, integrate, mutualInfo));

			System.out.println("DONE");
		}
	}

	public void getSubSet(List<Agent> subset, int currentPos, int limit){
		if(!subset.contains(agents.get(currentPos)))
			subset.add(agents.get(currentPos));
			//used.add(birds.get(currentPos));
		else
			System.out.println("USED FOUND");
		
		// Getting random subsets could have duplicates - needs work
		if(limit > 1){
			if(isRandom)
				getSubSet(subset, rand.nextInt(agents.size()), limit-1);
			else
				getSubSet(subset, currentPos+1,limit-1);
		}
	}

	public void getParticularSubset(List<Integer> agentIds){
		List<Agent> subset = new ArrayList<>();

		for(Agent currentAgent : agents){
			if(agentIds.contains(currentAgent.who)){
				subset.add(currentAgent);
			}
		}

		subsets.add(subset);
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