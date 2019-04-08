import java.io.*;
import java.util.*;

public class Driver {

	static String[] globals;

	private static boolean isFlockingSimulator(String simulationType) {
		if (simulationType.contains("Flocking")) {
			return true;
		}
		return false;
	}

	private static void Process(OpenModel open, ArrayList<FlockBird> birds, int discretize, String testInput,
			int maxSize, int numSubsets, int probSample) {
		List<Integer> state = open.getState();
		List<String[]> output = open.getTurtles();
		
		Driver.globals = open.getGlobals();
		
		Random rand = new Random();

		for (int i = 0; i < output.size(); i++) {

			for (int j = 0; j < output.get(i).length; j++) {
				output.get(i)[j] = output.get(i)[j].replace("\"", "");
			}

			birds.add(new FlockBird(Integer.parseInt(output.get(i)[0]), Double.parseDouble(output.get(i)[3]),
					Double.parseDouble(output.get(i)[4]),
					Integer.parseInt(output.get(i)[14].substring(8, output.get(i)[14].indexOf('}'))), discretize));

			birds.get(i).addFlockMates(output.get(i)[13].split(" "));
		}

		open.getHeadings(birds, testInput);
		HashMap<Integer, Integer> map = birds.get(0).getMap();
		int numHeadings = birds.get(0).getNumHeadings();
		Iterator iter = map.values().iterator();

		int count = 0;

		while (iter.hasNext()) {
			count = count + (int) iter.next();
		}

		List<CandidateSubset> candidates = new ArrayList<>();

		// Subsets start at size 2
		for (int i = 2; i <= maxSize; i++) {
			System.out.println("i: " + i);
			candidates.add(new CandidateSubset(birds, i, numSubsets, numHeadings, maxSize, probSample, rand));
			candidates.get(i - 2).calculateSubsets();
		}
	}

	public static void main(String[] args) {
		System.out.println(args[0]);

		String simulationType = args[0];
		int probSample = Integer.parseInt(args[1]); // Cannot be larger than 10 for my laptop.
		String testInput = args[2]; // Heading input
		int maxSize = Integer.parseInt(args[3]); // Maximum size of subset, keep it small (10)
		int discretize = Integer.parseInt(args[4]); // Resolution of data to look at (larger the better, 10 for my
													// laptop)
		int numSubsets = Integer.parseInt(args[5]); // Small for testing (2-3)

		System.out.println("DISCRETIZE:  " + discretize);

		if (Driver.isFlockingSimulator(simulationType)) {

			ArrayList<FlockBird> birds = new ArrayList<FlockBird>();

			OpenModel open = new OpenModel(args[0], "birds");

			Driver.Process(open, birds, discretize, testInput, maxSize, numSubsets, probSample);
			

		} else if (args[0].indexOf("TrafficBasic") != -1) {

		}
	}
}
