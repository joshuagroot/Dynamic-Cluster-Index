import java.util.*;
import java.lang.Math;
import java.math.BigInteger;
import java.util.concurrent.*;


public class Entropy{
	public double entropy;
	public int numHeadings;
	public int calcCount;
	public double mutualInfo;
	public int numThreads;
	int[] counters;

	private ExecutorService pool;
	private final Integer WORKSIZE = 100000000;

	private HashMap<String, Double> cache;

	public Entropy(int numHeadings, int numThreads){
		entropy = 0;
		this.numThreads = numThreads;
		this.numHeadings = numHeadings;
		this.cache = new HashMap<String, Double>(); //Initialising the simple cache
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
	public void anyProbParallel(List<List<Integer>> distribution){
		
		//System.out.println("SET SIZE: " + distribution.size());
		entropy = 0;
		calcCount = 0;
		BigInteger numWork = new BigInteger("1");
		long numCalcs = 1;
		pool = Executors.newFixedThreadPool(numThreads);
		counters = new int[distribution.size()];

		// PREVIOUS SOLUTION
		//Find the number of calculations that need to be done
		for(int i = 0; i < distribution.size(); i++){
			//Integer size = headings.get(i).size();
			numCalcs *= distribution.get(i).size();
			//numWork = numWork.multiply(new BigInteger(size.toString()));
		}
		//System.out.println("NUMCALCS: " + numCalcs);
		//System.out.println("NUMCALS DIVIDED: " + numCalcs/WORKSIZE);
		if(this.cache.get(distribution.toString()) != null) {
			//System.out.println("CACHE HIT\n\n\n");
			entropy = this.cache.get(distribution.toString());	//Set entropy and return
			return;
		} 

		//Store those calculations
		int[] bases = new int[distribution.size()];

		for(int i = 0; i < bases.length; i++){
			bases[i] = distribution.get(i).size();
		}

		// Threads will store their results here
		Vector<Double> result = new Vector<>();

		//PREVIOUS SOLUTION
		//Find out how much work each thread needs to do
		int spreadWork = (int)Math.ceil((double)numCalcs/(double)numThreads);
		System.out.println(spreadWork);
		if(spreadWork < 0){
			System.out.println("NUMCALCS: " + numCalcs);
			System.out.println("BIGNUM: " + numWork.intValue());
			System.exit(0);
		}

		// //PREVIOUS SOLUTION
		// //Execute the threads
		for(int i = 0; i < numThreads; i++){
			pool.execute(new ParallelEntropy(bases, result, i, distribution, numHeadings, spreadWork));
		}

		// BIGINTEGER SOLUTION
		// int workPositions = 0;
		// BigInteger subtraction = new BigInteger(WORKSIZE.toString());

		// while(numWork.min(subtraction) == subtraction){
		// 	pool.execute(new ParallelEntropy(bases, result, workPositions, headings, numHeadings, WORKSIZE));
		// 	numWork = numWork.subtract(subtraction);
		// 	//System.out.println(numWork.intValue());
		// 	workPositions++;
		// }
		// pool.execute(new ParallelEntropy(bases, result, workPositions, headings, numHeadings, numWork.intValue()));

		// WORK DISTRIBUTION SOLUTION
		// boolean done = false;
		// int count = 0;
		

		// for(int i = 0; i < numThreads; i++){
		// 	pool.execute(new ParallelEntropy(result, distribution, numHeadings, WORKSIZE, count, this));	
		// }
		// Send shut down orders
		pool.shutdown();

		// Still needs work here
		// Wait for pool to finish then shut down, otherwise shut it down manually
		try{
			pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		} catch(InterruptedException e){
			System.out.println("Took too long!");
			pool.shutdownNow();
		}
		//System.out.println("Collecting results");

		// Collect the results
		for(double i : result){
			entropy += i;
		}

		//Collections.sort(distribution);
		for(List<Integer> i : distribution){
			Collections.sort(i);
		}
		String key = distribution.toString();
		this.cache.put(key, entropy);
	}

	private boolean increment(List<List<Integer>> distribution){

		//printArray(counters);

		for(int i=distribution.size()-1; i>=0; i--){
			if(counters[i] <distribution.get(i).size()-1){
				counters[i]++;
				return true;
			} else {
				counters[i] = 0;
			}
		}
		System.out.println("RETURNING FALSE");
		return false;
	}

	synchronized public int[] askForWork(List<List<Integer>> distribution){
		int[] countersCopy = Arrays.copyOf(counters, counters.length);
		boolean done = false;

		for(int i = 0; i < WORKSIZE; i++){
			if(!increment(distribution)){
				done = true;
				System.out.println("DONE");
				break;
			}
		}

		if(done){
			System.out.println("MAIN THREAD DONE");
			return null;
		}
		//printArray(counters);

		return countersCopy;
	}

	public void printArray(int[] input){
		for(int i : input){
			System.out.print(i + " ");
		}
		System.out.println();
	}
	// Parallel version of the iterative solution, each thread receives some of the work
	class ParallelEntropy implements Runnable{

		private int[] counters;
		private Vector<Double> result;
		//private double[] result;
		private int[] bases;
		private int pos;
		private List<List<Integer>> distribution;
		private int numDist;
		private long numWork;
		private double entropy;
		private int calcCount = 0;
		private Entropy ent;

		// PREVIOUS SOLUTION
		public ParallelEntropy(int[] bases, Vector<Double> result, int pos, List<List<Integer>> distribution, int numDist, long numWork){
			//this.counters = counters;
			//this.bases = bases;
			counters = new int[distribution.size()];

			this.distribution = distribution;
			this.numDist = numDist;
			this.numWork = numWork;
			this.entropy = 0;
			this.result = result;
			this.pos = pos;

			if(numWork < 0){
				System.out.println("NUMWORK: " + numWork);
				System.exit(0);
			}

			// BigInteger numberOfWork = new BigInteger((Integer.valueOf(numWork).toString()));
			// BigInteger posInt = new BigInteger((Integer.valueOf(pos)).toString());
			// BigInteger spreadWork = new BigInteger(numberOfWork.multiply(posInt).toString());
			long spreadWork = numWork*pos;

			// if(spreadWork < 0){
			// 	System.out.println("Negative spreadwork: " + spreadWork + " numwork: " + numWork + " pos: " + pos + " together: " + numWork*pos);
			// 	System.exit(0);
			// }

			for(int i = distribution.size()-1; i >= 0 && spreadWork != 0; i--){
				// Fill out work array
				//BigInteger modVal = new BigInteger(Integer.valueOf(bases[i]).toString());
				//int modulo = spreadWork.mod(modVal).intValue();
				int modulo = (int)(spreadWork % bases[i]);
				long remainder = spreadWork/bases[i];

				try{
					counters[i] = modulo;
				} catch (IndexOutOfBoundsException e){
					//System.out.println("Thread found negative. modulo = " + modulo + " remainder: " + remainder + " bases at " + i + " is " + bases[i] + " spreadwork: " + spreadWork);
					System.exit(0);
				}
				//spreadWork = spreadWork.divide(new BigInteger(Integer.valueOf(bases[i]).toString()));
				spreadWork = remainder;
			}
		}

		//NO BIGINTEGER SOLUTION
		public ParallelEntropy(Vector<Double> result, List<List<Integer>> distribution, int numDist, int numWork, int pos, Entropy ent){
			this.counters = ent.askForWork(distribution);
			System.out.println("PRINTING COUNTERS");
			//printArray(counters);
			this.result = result;
			this.numDist = numDist;
			this.numWork = numWork;
			this.distribution = distribution;
			this.entropy = 0;
			this.pos = pos;
			this.ent = ent;
		}
 
		public void run(){
			//System.out.println("Thread: " + pos + " starting");
			// debugging
			double start_time = System.nanoTime();
			if(counters == null){
				return;
			}
			do{
				combination();
				numWork--;

				if(numWork == 0){
					// counters = ent.askForWork(distribution);
					// if(counters == null)
						break;
				}
				////System.out.println(entropy);
			} while(increment());

			double end_time = System.nanoTime();
			double secondDifference = (end_time - start_time) / 1e6;

			//System.out.println("Thread: " + pos + " Finished with: " + -entropy + " in: " + secondDifference);
			result.add((-entropy));
			//result[pos] = -entropy;
		}

		private boolean increment(){
			if(counters == null){
				return false;
			}
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
			//calcCount++;
			double temp = 1;
			for(int i = 0; i < distribution.size(); i++){

				try{
					temp *= (((double)distribution.get(i).get(counters[i]))/(double)numDist);
				} catch (IndexOutOfBoundsException e){
					System.out.println("Thread found negative. i: " + i + " counters[i]: " + counters[i] + " numDist: " + numDist + "  numwork: " + numWork + " bases[i]: " + bases[i]);
				}
			}

			entropy += (temp * (Math.log(temp)/Math.log(2)));
		}
	}

	
}