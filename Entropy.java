import java.util.*;
import java.lang.Math;
import java.util.concurrent.*;
import java.util.HashMap;

public class Entropy{
	public double entropy;
	public int numHeadings;
	public int calcCount;
	public double mutualInfo;
	public int numThreads;
	private ExecutorService pool;

	private HashMap<String, Double> cache;

	public Entropy(int numHeadings){
		entropy = 0;
		numThreads = 4;
		this.numHeadings = numHeadings;
		this.cache = new HashMap<String, Double>();
	}

	// Recursive solution
	public void anyProb(LinkedList<Double> previousValues, ArrayList<HashMap<Double, Integer>> headings){
		if (previousValues.size() < headings.size() - 1){
			Iterator i = headings.get(previousValues.size()).values().iterator();

			while (i.hasNext()){
				previousValues.addFirst(new Double((int)(i.next())));

				anyProb(previousValues, headings);
				previousValues.removeFirst();
			}
		} else{
			Iterator i = headings.get(previousValues.size()).values().iterator();
			entropy = 0;
			int count = 0;
			while (i.hasNext()){
				double temp = (int)i.next();
				temp = temp/numHeadings;
				for(int j = 0; j < previousValues.size(); j++){
					temp = temp * (previousValues.get(j)/numHeadings);
				}
				entropy += temp * (Math.log(temp)/Math.log(2));
			}
		}
	}

	//Iterative solution
	//https://stackoverflow.com/questions/16549831/all-possible-combination-of-n-sets
	public void anyProbIter(ArrayList<ArrayList<Integer>> headings){
		entropy = 0;
		calcCount = 0;

		int[] counters = new int[headings.size()];
		do{
			combination(counters, headings);
			calcCount++;
		} while(increment(counters, headings));
	}

	private boolean increment(int[] counters, ArrayList<ArrayList<Integer>> headings){
		for(int i=headings.size()-1; i>=0; i--){
			if(counters[i] <headings.get(i).size()-1){
				counters[i]++;
				return true;
			} else {
				counters[i] = 0;
			}
		}

		return false;
	}

	private void combination(int[] counters, ArrayList<ArrayList<Integer>> headings){
		double temp = 1.0;

		for(int i = 0; i < headings.size(); i++){
			temp *= (((double)headings.get(i).get(counters[i]))/numHeadings); //* ((Math.log((double)headings.get(i).get(counters[i]))/numHeadings)/Math.log(2));
			////System.out.println(Math.log(temp));
			//calcCount++;
		}

		entropy += temp * (Math.log(temp)/Math.log(2));
		//System.out.println(entropy);
	}

	public double mutualInformation(ArrayList<ArrayList<Integer>> firstSet, ArrayList<ArrayList<Integer>> secondSet){

		entropy = 0;
		mutualInfo = 0;

		// System.out.print("ITERATIVE: ");
		// long start_time = System.nanoTime();
		// anyProbIter(firstSet);
		// double hs = -entropy;
		// System.out.println(hs);
		// //System.out.println("ITERATIVE RESULT: " + hs);
		// long end_time = System.nanoTime();
		// double difference = (end_time - start_time) / 1e6;
		// //System.out.println("Hs of first set: " + hs);
		// entropy = 0;


		//System.out.print("PARALLEL: ");
		//start_time = System.nanoTime();
		anyProbParallel(firstSet);
		double first = entropy;
		System.out.println("PARALLEL RESULT: " + first);
		//end_time = System.nanoTime();
		//double secondDifference = (end_time - start_time) / 1e6;

		//System.out.println("ITERATIVE: " + hs + " PARALLEL " + test);
		//entropy = 0;

		//System.out.println("Iterative: " + difference + " parallel: " + secondDifference);
		//anyProbIter(secondSet);
		//double rest = entropy;
		entropy = 0;
		System.out.println(secondSet);
		anyProbParallel(secondSet);
		double rest = entropy;
		System.out.println(rest);
		//System.out.println("ITERATIVE: " + rest + " PARALLEL " + test);

		//System.exit(0);
		firstSet.addAll(secondSet);
		anyProbParallel(firstSet);
		double jointEntropy = entropy;

		mutualInfo = (first + rest) - jointEntropy;
		System.out.println("RETURING MUTUAL INFO: " + mutualInfo);
		return mutualInfo;
	}

	public double integration(ArrayList<FlockBird> birds, double givenEnt){

		double integrate = 0;

		for(int i = 0; i < birds.size(); i++){
			integrate += (birds.get(i).getEntropy() - givenEnt);
		}

		System.out.println("Integration: " + integrate);

		return integrate;
	}

	public void printArr(int[] arr){
		for(int i : arr){
			System.out.print(i + " ");
		}
		System.out.println();
	}

	// Parallel iterative solution
	public void anyProbParallel(ArrayList<ArrayList<Integer>> headings){
		
		//Check cache to see if entropy already calculated
		if(this.cache.get(headings.toString()) != null) {
			// System.out.println("CACHE HIT\n\n\n");
			entropy = this.cache.get(headings.toString());	//Set entropy and retur
			return;
		} else {
			// System.out.println("CACHE MISS\n\n\n");
		}

		entropy = 0;
		calcCount = 0;
		int numCalcs = 1;
		pool = Executors.newFixedThreadPool(numThreads);

		// Find the number of calculations that need to be done
		for(int i = 0; i < headings.size(); i++){
			numCalcs *= headings.get(i).size();
		}

		// Store those calculations
		int[] bases = new int[headings.size()];

		for(int i = 0; i < bases.length; i++){
			bases[i] = headings.get(i).size();
		}

		// Threads will store their results here
		double[] result = new double[numThreads];

		// Find out how much work each thread needs to do
		int spreadWork = (int)Math.ceil((double)numCalcs/(double)numThreads);

		// Execute the threads
		for(int i = 0; i < numThreads; i++){
			pool.execute(new ParallelEntropy(bases, result, i, headings, numHeadings, spreadWork));
		}

		// Send shut down order
		pool.shutdown();

		// Still needs work here
		// Wait for pool to finish then shut down, otherwise shut it down manually
		try{
			pool.awaitTermination(240, TimeUnit.SECONDS);
		} catch(InterruptedException e){
			System.out.println("Took too long!");
			pool.shutdownNow();
		}

		// Collect the results
		for(double i : result){
			entropy += i;
		}

		//Set cache TM
		String key = headings.toString();
		// System.out.println("Adding item: '" + key + "' with entropy '" + entropy + "'");
		this.cache.put(key, entropy);
	}

	// Parallel version of the iterative solution, each thread receives some of the work
	class ParallelEntropy implements Runnable{

		private int[] counters;
		private double[] result;
		private int pos;
		private ArrayList<ArrayList<Integer>> distribution;
		private int numDist;
		private int numWork;
		private double entropy;
		private int calcCount = 0;

		public ParallelEntropy(int[] bases, double[] result, int pos, ArrayList<ArrayList<Integer>> distribution, int numDist, int numWork){
			//this.counters = counters;
			counters = new int[distribution.size()];
			this.distribution = distribution;
			this.numDist = numDist;
			this.numWork = numWork;
			this.entropy = 0;
			this.result = result;
			this.pos = pos;

			int spreadWork = numWork*pos;

			for(int i = distribution.size()-1; i >= 0 && spreadWork != 0; i--){
				// Fill out work array
				int modulo = spreadWork % bases[i];
				int remainder = spreadWork/bases[i];

				if(modulo < 0) {
					modulo = 0;
				}
				counters[i] = modulo;
				spreadWork = remainder;
			}
		}
 
		public void run(){
			//System.out.println("Thread: " + pos + " starting");
			// debugging
			// double start_time = System.nanoTime();

			do{
				combination();
				numWork--;

				if(numWork == 0){
					break;
				}
				////System.out.println(entropy);
			} while(increment());

			//double end_time = System.nanoTime();
			//double secondDifference = (end_time - start_time) / 1e6;
			//System.out.println("Thread: " + pos + " Finished with: " + calcCount + " jobs and took: " + secondDifference);
			result[pos] = -entropy;
		}

		private boolean increment(){

			for(int i=distribution.size()-1; i>=0; i--){
				if(counters[i] <distribution.get(i).size()-1){
					counters[i]++;
					return true;
				} else {
					counters[i] = 0;
				}
			}

			return false;
		}

		private void combination(){
			float temp = 1;
			int i = 0;
			try{
			calcCount++;
			
			// System.out.println("NEW COMB");
			for(; i < distribution.size(); i++){
				// System.out.println(i);
				temp *= (
					(
						(float)distribution.get(i).get(
							counters[i]))/(float)numDist);
			}

			entropy += temp * (Math.log(temp)/Math.log(2));
			} catch(java.lang.IndexOutOfBoundsException e) {
				System.out.println("I: " + i);
				System.out.println("CALC COUNT:" + calcCount);
				System.out.println("DIST SIZE: " + distribution.size());
				System.out.println("DIST: " + distribution.toString());
				System.out.println("TEMP: " + temp);
				System.out.println("COUNTER SIZE: " + counters.length);
				System.out.println("COUNTERS: " + counters.toString());
				System.out.println("numDist: " + numDist);
				System.out.println(distribution.get(i).toString());
				System.out.println();
				System.out.println(e.toString());
				System.out.println("COUNTERS");
				for(int d = 0 ; d < counters.length ; d++) {
					System.out.println(counters[d]);
				}
				System.exit(1);

			}
		}
	}

	private void printArray(int[] input){
		for(int i : input){
			System.out.print(i + " ");
		}
		System.out.println();
	}
}