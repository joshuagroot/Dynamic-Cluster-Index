import java.util.*;
import java.lang.Math;
import java.util.concurrent.*;


public class Entropy{
	public double entropy;
	public int numHeadings;
	public int calcCount;
	public double mutualInfo;
	public int numThreads;
	private ExecutorService pool;

	public Entropy(int numHeadings){
		entropy = 0;
		numThreads = 6;
		this.numHeadings = numHeadings;
	}

	//Iterative solution
	//https://stackoverflow.com/questions/16549831/all-possible-combination-of-n-sets
	public void anyProbIter(List<List<Integer>> headings){
		entropy = 0;
		calcCount = 0;

		int[] counters = new int[headings.size()];
		do{
			combination(counters, headings);
			calcCount++;
		} while(increment(counters, headings));
	}

	private boolean increment(int[] counters, List<List<Integer>> headings){
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

	private void combination(int[] counters, List<List<Integer>> headings){
		double temp = 1.0;

		for(int i = 0; i < headings.size(); i++){
			temp *= (((double)headings.get(i).get(counters[i]))/numHeadings); //* ((Math.log((double)headings.get(i).get(counters[i]))/numHeadings)/Math.log(2));
			////System.out.println(Math.log(temp));
			//calcCount++;
		}

		entropy += temp * (Math.log(temp)/Math.log(2));
		//System.out.println(entropy);
	}

	public double mutualInformation(List<List<Integer>> firstSet, List<List<Integer>> secondSet){

		entropy = 0;
		mutualInfo = 0;

		anyProbParallel(firstSet);
		double first = entropy;

		entropy = 0;
		anyProbParallel(secondSet);
		double rest = entropy;

		firstSet.addAll(secondSet);
		anyProbParallel(firstSet);
		double jointEntropy = entropy;

		mutualInfo = (first + rest) - jointEntropy;
		return mutualInfo;
	}

	public double integration(List<FlockBird> birds, double givenEnt){

		double integrate = 0;

		for(int i = 0; i < birds.size(); i++){
			integrate += (birds.get(i).getEntropy() - givenEnt);
		}

		return integrate;
	}

	public void printArr(int[] arr){
		for(int i : arr){
			System.out.print(i + " ");
		}
		System.out.println();
	}

	// Parallel iterative solution
	public void anyProbParallel(List<List<Integer>> headings){
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

	}

	// Parallel version of the iterative solution, each thread receives some of the work
	class ParallelEntropy implements Runnable{

		private int[] counters;
		private double[] result;
		private int pos;
		private List<List<Integer>> distribution;
		private int numDist;
		private int numWork;
		private double entropy;
		private int calcCount = 0;

		public ParallelEntropy(int[] bases, double[] result, int pos, List<List<Integer>> distribution, int numDist, int numWork){
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
			calcCount++;
			float temp = 1;
			for(int i = 0; i < distribution.size(); i++){
				temp *= (((float)distribution.get(i).get(counters[i]))/(float)numDist);
			}

			entropy += temp * (Math.log(temp)/Math.log(2));
		}
	}

	private void printArray(int[] input){
		for(int i : input){
			System.out.print(i + " ");
		}
		System.out.println();
	}
}