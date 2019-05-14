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
		this.subSetsSize = subSetsSize;
		this.birds = birds;
		this.subSetLimit = subSetLimit;
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

		for(int i = 0; i < subsets.size(); i++){
			// Current subset and rest of the system
			listProbs = new ArrayList<ArrayList<Integer>>();
			ArrayList<ArrayList<Integer>> restOfListProbs = new ArrayList<ArrayList<Integer>>();

			for(int j = 0; j < subsets.get(i).size(); j++){
				listProbs.add(subsets.get(i).get(j).toList(probSample));
			}

			// Calculate the current subset
			// This is needed for integration, but is repeated in mutual information, needs a rework
			ent.anyProbParallel(listProbs);

			double hs = -ent.entropy;
			System.out.println("HS of CS: " + hs);

			ArrayList<FlockBird> restOfSystem = new ArrayList<FlockBird>();

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

			clusterIndex = integrate/mutualInfo;
			System.out.println(clusterIndex);
			System.out.println("DONE");
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