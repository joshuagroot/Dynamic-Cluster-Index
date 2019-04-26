import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public class Driver {

	static String[] globals;

	private static boolean isFlockingSimulator(String simulationType) {
		if (simulationType.contains("Flocking")) {
			return true;
		}
		return false;
	}

	private static void Process(OpenModel open, List<FlockBird> birds, int discretize, String headingsFile,
			int maxSize, int numSubsets, int probSample, boolean random) {
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

		open.getHeadings(birds, headingsFile);
		Map<Integer, Integer> map = birds.get(0).getMap();
		int numHeadings = birds.get(0).getNumHeadings();
		Iterator<Integer> iter = map.values().iterator();

		int count = 0;

		while (iter.hasNext()) {
			count = count + (int) iter.next();
		}

		List<CandidateSubset> candidates = new ArrayList<>();

		// Subsets start at size 2
		for (int i = 2; i <= maxSize; i++) {
			System.out.println("i: " + i);
			candidates.add(new CandidateSubset(birds, i, numSubsets, numHeadings, maxSize, probSample, rand, random));
			candidates.get(i - 2).calculateSubsets();
			// Break here for now
			break;
		}

		candidates.get(0).printSubsets();
	}

	public static void seive(List<CandidateSubset> candidates){

	}

	public static void main(String[] args) throws Exception{
		System.out.println(args[0]);

        JSONParser jsonParser = new JSONParser();
        JSONObject inputList;

		FileReader jsonInput = new FileReader(args[0]);
		Object obj = jsonParser.parse(jsonInput);
		inputList = (JSONObject) obj;
		System.out.println(inputList);

		String simulationType = (String)inputList.get("BirdObjectData");
		int probSample = (int)(long)inputList.get("probSample");
		String headingsFile = (String)inputList.get("movementData");
		int maxSize = (int)(long)inputList.get("maxSize");
		int discretize = (int)(long)inputList.get("discretize");
		int numSubsets = (int)(long)inputList.get("numSubSets");
		boolean random = (boolean)inputList.get("random");

		if (Driver.isFlockingSimulator(simulationType)) {

			List<FlockBird> birds = new ArrayList<>();

			OpenModel open = new OpenModel(simulationType, "birds");

			Driver.Process(open, birds, discretize, headingsFile, maxSize, numSubsets, probSample, random);
			

		}
	}
}
