import java.util.*;
import java.lang.Math;

// Each candidateSubset object holds the DCI for each subset size - e.g. 2, 3, 4...
public class CandidateSubset{

	public int numHeadings;
	public Entropy ent;
	public int subSetsSize;
	public int subSetLimit;
	public int maxSize;
	public int probSample;
	public double clusterIndex;
	public Random rand;
	public ArrayList<FlockBird> birds;
	public ArrayList<ArrayList<FlockBird>> subsets;
	public ArrayList<ArrayList<Integer>> listProbs;
	public ArrayList<Integer> subsetValues;

	public CandidateSubset(ArrayList<FlockBird> birds, int subSetsSize, int subSetLimit, int numHeadings, int maxSize, int probSample, Random rand){

		this.numHeadings = numHeadings;
		this.subSetsSize = subSetsSize;	//Maximum size of a subset
		this.birds = birds;
		this.subSetLimit = subSetLimit;	//Limit on number of subsets to consider
		this.maxSize = maxSize;
		this.probSample = probSample;
		this.clusterIndex = 0;
		this.rand = rand;

		ent = new Entropy(numHeadings);
		subsets = new ArrayList<ArrayList<FlockBird>>();
		subsetValues = new ArrayList<Integer>();
	}

	private void generateSubsets() {
		//TODO: Clean this up - potentially remove the inner getSubSet recursive logic
		for(int subSetIndex = 0; subSetIndex < subSetLimit; subSetIndex++){
			ArrayList<FlockBird> temp = new ArrayList<FlockBird>();
			getSubSet(temp, rand.nextInt(birds.size()), subSetsSize);
			subsets.add(temp);
		}
		System.out.println("subsets generated");
	}

	// Calculate the DCI for subsets of this size - work in progress
	public void calculateSubsets(){
		System.out.println("Calculating Subset");
		// Build subsets
		this.generateSubsets();

		System.out.println("Number of subsets to consider: " + subsets.size() + "\n");
		for(int i = 0; i < subsets.size(); i++){
			ArrayList<FlockBird> currentSubset = subsets.get(i);

			// Current subset and rest of the system
			listProbs = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Integer>> restOfListProbs = new ArrayList<ArrayList<Integer>>();

			for(int j = 0; j < currentSubset.size(); j++){
				listProbs.add(currentSubset.get(j).toList(probSample));
			}

			// This is needed for integration, but is repeated in mutual information, needs a rework (TODO duplicated code, cache in Entropy?)
			ent.anyProbParallel(listProbs);
			//The previous call waits for the pool of Runnables to stop

			double hs = -ent.entropy;	//Flip the sign
			System.out.println("HS of CS: " + hs);	//TODO: Can this be replicated on a paper system?

			// ArrayList<FlockBird> restOfSystem = new ArrayList<FlockBird>(birds);
			
			ArrayList<FlockBird> restOfSystem = new ArrayList<FlockBird>();
			// restOfSystem.removeAll(currentSubset);

			// Add the rest of the system to the second set, only up to maxSize though
			//TODO: Use this instead - secondList.removeAll(firstList);
			// Why is maxSize being used here? ANS: Used for performance
			
			for(int birdIterator = 0; birdIterator < maxSize; birdIterator++){
				FlockBird current = birds.get(birdIterator);

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
				restOfListProbs.add(restOfSystem.get(j).toList(probSample));
			}

			double mutualInfo= ent.mutualInformation(listProbs, restOfListProbs);
			double integrate = ent.integration(currentSubset, hs);

			clusterIndex = integrate/mutualInfo;
			System.out.println("CLUSTER INDEX:	" + clusterIndex + "\nSubset considered\n");
		}
	}

	public void getSubSet(ArrayList<FlockBird> subset, int currentPos, int limit){
		subset.add(birds.get(currentPos));
		subsetValues.add(birds.get(currentPos).who);

		// Getting random subsets could have duplicates - needs work
		if(limit != 1){
			getSubSet(subset, rand.nextInt(birds.size()), limit-1);
		}
	}
	public void printSubsets(){
		for(int i = 0; i < subsets.size(); i++){
			for(int j = 0; j < subsets.get(i).size(); j++){
				System.out.print(subsets.get(i).get(j).who + ", ");
			}
			System.out.println();

		}
	}
}