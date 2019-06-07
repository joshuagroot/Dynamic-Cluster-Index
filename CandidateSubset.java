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
	List<FlockBird> birds;
	List<List<FlockBird>> subsets;
	List<List<Integer>> listProbs;
	List<Integer> subsetValues;
	List<Double> dciValues;
	List<Candidate> candidateSubsets;

	public CandidateSubset(List<FlockBird> birds, int subSetsSize, int subSetLimit, int numHeadings, 
		int maxSize, int probSample, int numThreads, Random rand, boolean isRandom){

		this.numHeadings = numHeadings;
		this.subSetsSize = subSetsSize;
		this.birds = birds;
		this.subSetLimit = subSetLimit;
		this.maxSize = maxSize;
		this.probSample = probSample;
		this.clusterIndex = 0;
		this.rand = rand;
		this.isRandom = isRandom;

		ent = new Entropy(numHeadings, numThreads);
		subsets = new ArrayList<>();
		dciValues = new ArrayList<>();
		candidateSubsets = new ArrayList<>();
	}

	// Calculate the DCI for subsets of this size - work in progress
	public void calculateSubsets(){
		System.out.println("Calculating Subset");
		// Build subsets
		for(int i = 0; i < subSetLimit; i++){

			List<FlockBird> temp = new ArrayList<>();
			if(isRandom)
				getSubSet(temp, rand.nextInt(birds.size()), subSetsSize, new ArrayList<FlockBird>());
			else{
				//System.out.println("We are here " + subSetsSize);
				getSubSet(temp, i, subSetsSize, new ArrayList<FlockBird>());
			}
			subsets.add(temp);
		}
		//System.out.println(subsetValues);
		//	System.out.println("subsets retrieved");

		for(int i = 0; i < subsets.size(); i++){
			// Current subset and rest of the system
			listProbs = new ArrayList<>();
			List<List<Integer>> restOfListProbs = new ArrayList<>();

			for(int j = 0; j < subsets.get(i).size(); j++){
				listProbs.add(subsets.get(i).get(j).toList(probSample));
			}

			// Calculate the current subset
			// This is needed for integration, but is repeated in mutual information, needs a rework
			ent.anyProbParallel(listProbs);

			double hs = -ent.entropy;
			//System.out.println("HS of CS: " + hs);

			List<FlockBird> restOfSystem = new ArrayList<FlockBird>();

			// Add the rest of the system to the second set, only up to maxSize though
			for(int j = 0; j < maxSize; j++){
				boolean found = false;
				for(int k = 0; k < subsets.get(i).size(); k++){
					if(subsets.get(i).get(k).who == birds.get(j).who){
						found = true;
						break;
					}
				}
				if(!found){
					restOfSystem.add(birds.get(j));
				}
			}

			for(int j = 0; j < restOfSystem.size(); j++){
				restOfListProbs.add(restOfSystem.get(j).toList(probSample));
			}

			//System.out.println("MUTUAL INFO");
			double mutualInfo= ent.mutualInformation(listProbs, restOfListProbs);
			//System.out.println("INTEGRATION");
			double integrate = ent.integration(subsets.get(i), hs);

			subsetValues = new ArrayList<>();

			for(int j = 0; j < subsets.get(i).size(); j++){
				subsetValues.add(subsets.get(i).get(j).who);
			}

			clusterIndex = integrate/mutualInfo;
			candidateSubsets.add(new Candidate(subsetValues, clusterIndex));

			System.out.println(clusterIndex);
			System.out.println("DONE");
		}
	}

	public void getSubSet(List<FlockBird> subset, int currentPos, int limit, List<FlockBird> used){
		if(!subset.contains(birds.get(currentPos)))
			subset.add(birds.get(currentPos));
			//used.add(birds.get(currentPos));
		else
			System.out.println("USED FOUND");
		
		// Getting random subsets could have duplicates - needs work
		if(limit > 1){
			if(isRandom)
				getSubSet(subset, rand.nextInt(birds.size()), limit-1, used);
			else
				getSubSet(subset, currentPos+1,limit-1, used);
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