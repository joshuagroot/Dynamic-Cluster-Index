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

	// Read in a list of candidate subsets from a JSON file
	public static List<Candidate> readCandidates(String fileName) throws Exception{
		List<Candidate> candidateSet = new ArrayList<>();
		JSONParser jsonParser = new JSONParser();
		FileReader jsonInput = new FileReader(fileName);

		Object obj = jsonParser.parse(jsonInput);
		JSONObject inputList = (JSONObject) obj;
		JSONArray jsonArray = (JSONArray) inputList.get("DCIList");

		for(int i = 0; i < jsonArray.size(); i++){
			List<Integer> currentCandidateAgents = new ArrayList<>();
			Candidate currentCandidate;

			JSONObject item = (JSONObject) jsonArray.get(i);
			String agentsAsString = (String) item.get("agents");
			String[] stringItems = agentsAsString.split(" ");

			for(String agentToInt : stringItems){
				currentCandidateAgents.add(Integer.parseInt(agentToInt));
			}
			double currentDCI = Double.parseDouble((String)item.get("DCI"));
			currentCandidate = new Candidate(currentCandidateAgents, currentDCI);
			candidateSet.add(currentCandidate);
		}

		return candidateSet;
	}

	// Sieving algorithm to remove overlapping subsets. 
	// Checks all pairs of subsets and if overlap is found, removes the subset with the lower DCI value
	public static List<Candidate> sieve(List<Candidate> candidates){
		List<Candidate> returnList = new ArrayList<>();
		int numCandidates = candidates.size();

		boolean[] deleted = new boolean[numCandidates];

		for(int i = 0; i < numCandidates-1; i++){
			for(int j = i+1; j < numCandidates; j++){
				if(deleted[i] != true && deleted[j] != true){
					if(isSubset(candidates.get(i), candidates.get(j))){
						if(candidates.get(i).getDci() > candidates.get(j).getDci())
							deleted[j] = true;
						else
							deleted[i] = true;
					}
				}
			}
		}

		for(int i = 0; i < deleted.length; i++){
			if(!deleted[i]){
				returnList.add(candidates.get(i));
			}
		}

		return returnList;
	}

	private static void Process(OpenModel open, List<FlockBird> birds, int discretize, String headingsFile,
			int maxSize, int numSubsets, int probSample, boolean random) {
		List<Integer> state = open.getState();
		List<String[]> output = open.getTurtles();

		//Queue<Candidate> orderedCandidates = new PriorityQueue<Candidate>(new CandidateComparison());
		
		Driver.globals = open.getGlobals();
		
		Random rand = new Random();

		//Iterate over Turtles/Agents/Birds
		for (int i = 0; i < output.size(); i++) {

			//Remove escaped quotation marks
			for (int j = 0; j < output.get(i).length; j++) {
				output.get(i)[j] = output.get(i)[j].replace("\"", "");
			}

			String param = output.get(i)[14];
			Integer subParam = 0;

			//Handling: Non-flocking simulations do not have flockmates
			if(param.length() > 2) {

				subParam = Integer.parseInt(output.get(i)[14].substring(
							8, 
							output.get(i)[14].indexOf('}')
						));
			}

			//Create bird from data
			birds.add(new FlockBird(Integer.parseInt(output.get(i)[0]), Double.parseDouble(output.get(i)[3]),
					Double.parseDouble(output.get(i)[4]),
					subParam, 
					discretize));

			//Add flock mates to bird object (remove for generic purposes)
			birds.get(i).addFlockMates(output.get(i)[13].split(" "));
			// System.out.println(birds.get(i).who);
		}


		open.getHeadings(birds, headingsFile);
		Map<Integer, Integer> map = birds.get(0).getMap();
		int numHeadings = birds.get(0).getNumHeadings();	//Get the number of unique headings for the bird
		Iterator<Integer> iter = map.values().iterator();

		List<CandidateSubset> candidateSubsets = new ArrayList<>();
		List<List<Candidate>> candidates = new ArrayList<>();

		// Subsets start at size 2
		for (int i = 2; i <= maxSize; i++) {

			System.out.println("i: " + i);
			candidateSubsets.add(new CandidateSubset(
				birds, 			// The list of FlockBird ojects representing agents in the system
				i, 				// Passed to 'subsetsSize' - maximum size of a subset
				numSubsets, 	// Number of subsets to generate
				numHeadings,	// Number of headings - used to create Entropy object
				maxSize, 		// The maximum size of the 'rest of the system' internal variable - used to limit calculations for performance gains
				probSample, 	// How many heading probabilities to consider per agent
				rand, 
				random
			));
			candidateSubsets.get(i - 2).calculateSubsets();

			candidates.add(candidateSubsets.get(i-2).getCandidates());
			// Break here for now
			break;
		}

		candidateSubsets.get(0).printSubsets();
		Collections.sort(candidates.get(0), new CandidateComparison());

		// System.out.println(candidates.get(0));
		// System.out.println(sieve(candidates.get(0)));
	}

	// Check if one candidate is wholly contained within another
	public static boolean isSubset(Candidate first, Candidate second){
		Set<Integer> firstAgents = first.getSet();
		Set<Integer> secondAgents = second.getSet();

		return firstAgents.containsAll(secondAgents) || secondAgents.containsAll(firstAgents);
	}

	public static void testSieving() throws Exception{
		List<Candidate> candidates = readCandidates("savedDCI/testDCI.json");

		candidates.forEach((i) -> System.out.println(i));

		//candidates.sort((Candidate a, Candidate b) -> a.getDci() - b.getDci());
		Collections.sort(candidates, (Candidate a, Candidate b) -> { 
				if(a.getDci() > b.getDci()){
					return 1;
				}
				else if (a.getDci() == b.getDci()) {
					return 0;
				}
				return -1;
		});

		System.out.println("SORTED");

		candidates.forEach((i) -> System.out.println(i));

		System.out.println("SIEVING");
		candidates = sieve(candidates);

		candidates.forEach((i) -> System.out.println(i));

		System.out.println("SIEVED");
	}

	public static void main(String[] args) throws Exception{

		// testSieving();

		System.out.println(args[0]);


        JSONParser jsonParser = new JSONParser();
        JSONObject inputList;

		FileReader jsonInput = new FileReader(args[0]);
		Object obj = jsonParser.parse(jsonInput);
		inputList = (JSONObject) obj;
		System.out.println(inputList);

		// TODO: Move these comments to the below parameters
		// int probSample = Integer.parseInt(args[1]); // Cannot be larger than 10 for my laptop.
		// String testInput = args[2]; 				// Heading input
		// int maxSize = Integer.parseInt(args[3]); 	// Maximum size of subset, keep it small (10)
		// int discretize = Integer.parseInt(args[4]); // Resolution of data to look at (larger the better, 10 for my laptop)
		// int numSubsets = Integer.parseInt(args[5]); // Small for testing (2-3)

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
