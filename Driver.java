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
			double time = new Double(item.get("time").toString());
			double integration = new Double(item.get("Integration").toString());
			double mutualInfo = new Double(item.get("MutualInformation").toString());

			currentCandidate = new Candidate(currentCandidateAgents, currentDCI, 0, integration, mutualInfo);
			candidateSet.add(currentCandidate);
		}
		return candidateSet;
	}

	public static void sortByDCI(List<Candidate> input){
		Collections.sort(input, new CandidateComparison());
	//	System.out.println(input);
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

	public static void printFlockMates(List<FlockBird> birds, String fileName){
		// List<Integer> usedBirds = new ArrayList<>();
		JSONArray jsonArray = new JSONArray();

		for(int i = 0; i < birds.size(); i++){

			int mostUsed = 0;
			int most = 0;
			int secondMostUsed = 0;
			int secondMost = 0;

			Map<Integer, Integer> currentFlockMates = birds.get(i).flockMateFrequency;
			Iterator<Integer> frequencyKeys = currentFlockMates.keySet().iterator();

			while(frequencyKeys.hasNext()){
				int currentMate = frequencyKeys.next();
				//System.out.println("CURRENT MATE: " + currentMate);

				if(currentFlockMates.get(currentMate) > most){
					if(most > secondMost){
						secondMost = most;
						secondMostUsed = mostUsed;
					}
					most = currentFlockMates.get(currentMate);
					mostUsed = currentMate;

				} else if(currentFlockMates.get(currentMate) > secondMost){
					secondMost = currentFlockMates.get(currentMate);
					secondMostUsed = currentMate;
				}
			}

			JSONObject currentBird = new JSONObject();

			currentBird.put("who", birds.get(i).who);
			currentBird.put("bestmate", mostUsed);
			currentBird.put("secondbestmate", secondMostUsed);

			jsonArray.add(currentBird);
		}

		try (FileWriter file = new FileWriter(fileName)) {

			file.write(jsonArray.toJSONString());
			file.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void Process(OpenModel open, List<FlockBird> birds, int discretize, String headingsFile,
			int maxSize, int totalSize, int numSubsets, int probSample, int numThreads, boolean random, String outputFile, String flockOutput) {
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
			//System.out.println(output.get(i)[14]);
			String neighbour = output.get(i)[14];
			if(neighbour.length() > 8)
				neighbour = neighbour.substring(8, output.get(i)[14].indexOf('}'));

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
					Integer.parseInt(neighbour), discretize));

			//System.out.println(birds.get(i).who);
		}


		open.getFlockMatesAndHeadings(birds, headingsFile);

		printFlockMates(birds, flockOutput);

		Map<Integer, Integer> map = birds.get(0).getMap();
		int numHeadings = birds.get(0).getNumHeadings();	//Get the number of unique headings for the bird
		Iterator<Integer> iter = map.values().iterator();

		List<Agent> birdAgents = new ArrayList<>();
		for(FlockBird bird : birds){
			birdAgents.add((Agent)bird);
		}

		List<CandidateSubset> candidateSubsets = new ArrayList<>();
		List<List<Candidate>> candidates = new ArrayList<>();

		// Subsets start at size 2
		JSONArray jsonCandidates = new JSONArray();
		int count = 0;

		candidateSubsets.add(new CandidateSubset(birdAgents, 2, numSubsets, numHeadings, maxSize, probSample, numThreads, rand, random));
		List<Integer> specificAgents = new ArrayList<>();
		specificAgents.add(2);
		specificAgents.add(11);
		// specificAgents.add(17);
		// specificAgents.add(7);
		// specificAgents.add(3);
		// specificAgents.add(6);
		// specificAgents.add(21);

		// candidateSubsets.get(0).getParticularSubset(specificAgents);
		// candidateSubsets.get(0).calculateDCI();
		for (int i = 2; i <= totalSize; i++) {
			double start_time = System.nanoTime();

			System.out.println("i: " + i);
			candidateSubsets.add(new CandidateSubset(birdAgents, i, numSubsets, numHeadings, maxSize, probSample, numThreads, rand, random));
			candidateSubsets.get(count).calculateSubsets();
			candidateSubsets.get(count).calculateDCI();
			candidates.add(candidateSubsets.get(count).getCandidates());

			for(int j = 0; j < candidates.get(count).size(); j++){

				JSONObject currentCandidate = new JSONObject();
				currentCandidate.put("DCI", Double.toString(candidates.get(count).get(j).getDci()));
				currentCandidate.put("agents", candidates.get(count).get(j).getAgents().toString());
				currentCandidate.put("integration", candidates.get(count).get(j).getIntegration());
				currentCandidate.put("MutualInformation", candidates.get(count).get(j).getMutualInfo());

				double time = candidates.get(count).get(j).getTime();
				currentCandidate.put("time", time);

				JSONObject currentFlockMates = new JSONObject();
				//System.out.println("PRINTING CANDIDATE");
				//System.out.println(currentCandidate);
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

			count++;
		}

		//candidateSubsets.get(0).printSubsets();
		//Collections.sort(candidates.get(0), new CandidateComparison());
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

				//System.out.println("PRINTING CANDIDATE");
				//System.out.println(currentCandidate);
				jsonCandidates.add(currentCandidate);
		//	}
		}
		
		//currentCandidate.put("DCI", candidates.get())
		// Break here for now
		//break;
		//System.out.println("PRINTING CANDIATE ARRAY");
		//System.out.println(jsonCandidates);
		try (FileWriter file = new FileWriter("savedDCI/phoenixTest.json")) {

			file.write(jsonCandidates.toJSONString());
			file.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeToCSV(List<Candidate> candidates) throws Exception{
		FileWriter csvWriter = new FileWriter("CSV/200Birds.csv");
		csvWriter.append("DCI");
		csvWriter.append(",");
		csvWriter.append("Agents");
		csvWriter.append(",");
		csvWriter.append("time");
		csvWriter.append("\n");

		for(Candidate currentCandidate : candidates){
			if(currentCandidate.getDci() > 0){
				List<Integer> currentAgents = currentCandidate.getAgents();
				String agentOutput = "";

				for(Integer agent : currentAgents){
					agentOutput += agent + " ";
				}
				csvWriter.append(String.valueOf(currentCandidate.getDci()));
				csvWriter.append(",");
				csvWriter.append(agentOutput);
				csvWriter.append(",");
				csvWriter.append(String.valueOf(currentCandidate.getTime()));
				csvWriter.append("\n");
			}
		}

		csvWriter.flush();

	}

	// Check if one candidate is wholly contained within another
	public static boolean isSubset(Candidate first, Candidate second){
		Set<Integer> firstAgents = first.getSet();
		Set<Integer> secondAgents = second.getSet();

		return firstAgents.containsAll(secondAgents) || secondAgents.containsAll(firstAgents);
	}

	// public static void testSieving() throws Exception{
	// 	List<Candidate> candidates = readCandidates("savedDCI/testDCI.json");

	// 	candidates.forEach((i) -> System.out.println(i));

	// 	//candidates.sort((Candidate a, Candidate b) -> a.getDci() - b.getDci());
	// 	Collections.sort(candidates, (Candidate a, Candidate b) -> { 
	// 			if(a.getDci() > b.getDci()){
	// 				return 1;
	// 			}
	// 			else if (a.getDci() == b.getDci()) {
	// 				return 0;
	// 			}
	// 			return -1;
	// 	});

	// 	System.out.println("SORTED");

	// 	candidates.forEach((i) -> System.out.println(i));
	// 	System.out.println("SIEVING");
	// 	candidates = sieve(candidates);
	// 	candidates.forEach((i) -> System.out.println(i));

	// 	System.out.println("SIEVED");
	// }

	public static void main(String[] args) throws Exception{

		// testSieving();
		// List<Candidate> candidates = testRead("savedDCI/prettyBirds.json");
		// System.out.println(candidates);
		// sortByDCI(candidates);
		// System.out.println("\nSORTED LIST\n");
		// System.out.println(candidates);
		// writeToCSV(candidates);

		// System.exit(0);
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

		// TODO: Move these comments to the below parameters
		// int probSample = Integer.parseInt(args[1]); // Cannot be larger than 10 for my laptop.
		// String testInput = args[2]; 				// Heading input
		// int maxSize = Integer.parseInt(args[3]); 	// Maximum size of subset, keep it small (10)
		// int discretize = Integer.parseInt(args[4]); // Resolution of data to look at (larger the better, 10 for my laptop)
		// int numSubsets = Integer.parseInt(args[5]); // Small for testing (2-3)

		String simulationType = (String)inputList.get("BirdObjectData");
		int probSample = ((Long)inputList.get("probSample")).intValue();
		String headingsFile = (String)inputList.get("movementData");
		String output = (String)inputList.get("output");
		String flockOutput = (String)inputList.get("flockMateOutput");
		int maxSize = ((Long)inputList.get("maxSize")).intValue();
		int discretize = ((Long)inputList.get("discretize")).intValue();
		int numSubsets = ((Long)inputList.get("numSubSets")).intValue();
		int totalSize = ((Long)inputList.get("totalSets")).intValue();
		int numThreads = ((Long)inputList.get("totalSets")).intValue();
		boolean random = (boolean)inputList.get("random");

		if (Driver.isFlockingSimulator(simulationType)) {

			List<FlockBird> birds = new ArrayList<>();

			OpenModel open = new OpenModel(simulationType, "birds");

			Driver.Process(open, birds, discretize, headingsFile, maxSize, totalSize, numSubsets, probSample, numThreads, random, output, flockOutput);
			

		}
	}
}
