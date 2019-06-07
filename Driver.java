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

	public static List<Candidate> testRead(String fileName) throws Exception{
		List<Candidate> candidateSet = new ArrayList<>();
		JSONParser jsonParser = new JSONParser();
		FileReader jsonInput = new FileReader(fileName);

		Object obj = jsonParser.parse(jsonInput);
		//JSONObject inputList = (JSONObject) obj;
		JSONArray jsonArray = (JSONArray) obj;

		//System.out.println("PRINTING READ ARRAY");
		//System.out.println(jsonArray);

		for(int i = 0; i < jsonArray.size(); i++){
			List<Integer> currentCandidateAgents = new ArrayList<>();
			Candidate currentCandidate;

			JSONObject item = (JSONObject) jsonArray.get(i);
			String agentsAsString = (String) item.get("agents");
			//System.out.println("AGENTS");

			String[] stringItems = agentsAsString.split(" ");

			for(String agentToInt : stringItems){
				//System.out.println(agentToInt);
				currentCandidateAgents.add(Integer.parseInt(agentToInt.replaceAll("[^\\d]", "" )));
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
			if(candidates.get(i).getDci() == Double.POSITIVE_INFINITY){
						deleted[i] = true;
						continue;
			}
			for(int j = i+1; j < numCandidates; j++){
				if(candidates.get(j).getDci() == Double.POSITIVE_INFINITY){
					deleted[j] = true;
				}
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
			int maxSize, int totalSize, int numSubsets, int probSample, int numThreads, boolean random, String outputFile) {
		List<Integer> state = open.getState();
		List<String[]> output = open.getTurtles();

		//Queue<Candidate> orderedCandidates = new PriorityQueue<Candidate>(new CandidateComparison());
		
		Driver.globals = open.getGlobals();
		
		Random rand = new Random();

		for (int i = 0; i < output.size(); i++) {

			for (int j = 0; j < output.get(i).length; j++) {
				output.get(i)[j] = output.get(i)[j].replace("\"", "");
			}
			System.out.println(output.get(i)[14]);
			String neighbour = output.get(i)[14];
			if(neighbour.length() > 8)
				neighbour = neighbour.substring(8, output.get(i)[14].indexOf('}'));

			birds.add(new FlockBird(Integer.parseInt(output.get(i)[0]), Double.parseDouble(output.get(i)[3]),
					Double.parseDouble(output.get(i)[4]),
					Integer.parseInt(neighbour), discretize));

			String flockmates = output.get(i)[13];
			if(flockmates != "turtles")
				birds.get(i).addFlockMates(output.get(i)[13].split(" "));
			System.out.println(birds.get(i).who);
		}

		open.getHeadings(birds, headingsFile);
		Map<Integer, Integer> map = birds.get(0).getMap();
		int numHeadings = birds.get(0).getNumHeadings();
		Iterator<Integer> iter = map.values().iterator();

		List<CandidateSubset> candidateSubsets = new ArrayList<>();
		List<List<Candidate>> candidates = new ArrayList<>();

		// Subsets start at size 2
		JSONArray jsonCandidates = new JSONArray();
		int count = 0;
		for (int i = 2; i <= totalSize; i++) {
			double start_time = System.nanoTime();

			System.out.println("i: " + i);
			candidateSubsets.add(new CandidateSubset(birds, i, numSubsets, numHeadings, maxSize, probSample, numThreads, rand, random));
			candidateSubsets.get(count).calculateSubsets();

			candidates.add(candidateSubsets.get(count).getCandidates());

			for(int j = 0; j < candidates.get(count).size(); j++){
				double sub_start_time = System.nanoTime();

				JSONObject currentCandidate = new JSONObject();
				currentCandidate.put("DCI", Double.toString(candidates.get(count).get(j).getDci()));
				currentCandidate.put("agents", candidates.get(count).get(j).getAgents().toString());

				//System.out.println("PRINTING CANDIDATE");
				//System.out.println(currentCandidate);
				double sub_end_time = System.nanoTime();

				currentCandidate.put("time", sub_end_time - sub_start_time);
				jsonCandidates.add(currentCandidate);

			}
			// entire size of model
			// size dci cluster
			// runtime it took
			// Good definition of good enough
			// model size on left
			// dci value on right
			// size

			//currentCandidate.put("DCI", candidates.get())
			// Break here for now
			//break;
			//System.out.println("PRINTING CANDIATE ARRAY");
			//System.out.println(jsonCandidates);
			try (FileWriter file = new FileWriter(outputFile)) {

				file.write(jsonCandidates.toJSONString());
				file.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
			double end_time = System.nanoTime();

			count++;
		}

		

		candidateSubsets.get(0).printSubsets();
		Collections.sort(candidates.get(0), new CandidateComparison());

		//System.out.println(candidates.get(0));
		//System.out.println(sieve(candidates.get(0)));
	}

	public static void writeJson(List<Candidate> candidates){
		JSONArray jsonCandidates = new JSONArray();

		for(int i = 0; i < candidates.size(); i++){
			//for(int j = 0; j < candidates.get(i).size(); j++){
				JSONObject currentCandidate = new JSONObject();
				currentCandidate.put("DCI", Double.toString(candidates.get(i).getDci()));
				currentCandidate.put("agents", candidates.get(i).getAgents().toString());

				System.out.println("PRINTING CANDIDATE");
				System.out.println(currentCandidate);
				jsonCandidates.add(currentCandidate);
		//	}
		}
		
		//currentCandidate.put("DCI", candidates.get())
		// Break here for now
		//break;
		System.out.println("PRINTING CANDIATE ARRAY");
		System.out.println(jsonCandidates);
		try (FileWriter file = new FileWriter("savedDCI/phoenixTest.json")) {

			file.write(jsonCandidates.toJSONString());
			file.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
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

		// List<Candidate> candidates = testRead("savedDCI/25Birds.json");

		// System.out.println("FINAL SET\n" + candidates + "\n");
		// candidates = sieve(candidates);
		// System.out.println("FINAL SET AFTER SIEVE: " + candidates);
		// writeJson(candidates);
		// System.exit(0);
		//System.out.println(args[0]);

        JSONParser jsonParser = new JSONParser();
        JSONObject inputList;

		FileReader jsonInput = new FileReader(args[0]);
		Object obj = jsonParser.parse(jsonInput);
		inputList = (JSONObject) obj;
		System.out.println(inputList);

		String simulationType = (String)inputList.get("BirdObjectData");
		int probSample = ((Long)inputList.get("probSample")).intValue();
		String headingsFile = (String)inputList.get("movementData");
		String output = (String)inputList.get("output");
		int maxSize = ((Long)inputList.get("maxSize")).intValue();
		int discretize = ((Long)inputList.get("discretize")).intValue();
		int numSubsets = ((Long)inputList.get("numSubSets")).intValue();
		int totalSize = ((Long)inputList.get("totalSets")).intValue();
		int numThreads = ((Long)inputList.get("totalSets")).intValue();
		boolean random = (boolean)inputList.get("random");

		if (Driver.isFlockingSimulator(simulationType)) {

			List<FlockBird> birds = new ArrayList<>();

			OpenModel open = new OpenModel(simulationType, "birds");

			Driver.Process(open, birds, discretize, headingsFile, maxSize, totalSize, numSubsets, probSample, numThreads, random, output);
			

		}
	}
}
