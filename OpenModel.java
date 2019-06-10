
import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
 

public class OpenModel {
	private String name;
	private String type;
	private String line = "";
	private BufferedReader br = null;
	private String cvsSplitBy = ",";
	private List<String> fileCache = new ArrayList<String>();

	private List<String> getRowsFromFile(String filename) {
		List<String> results = new ArrayList<>();

		try {
			br = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			while ((line = br.readLine()) != null) {
				results.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}
	public String getName() {
		return this.name;
	}

	public OpenModel(String name, String type) {
		this.name = name;
		this.type = type;
		this.fileCache = getRowsFromFile(this.name);
	}

	public List<Integer> getState() {
		List<Integer> state = new ArrayList<>();

		int headerIndex = -1;
		// Find the area that follows "Random state" header line
		for (int index = 0; index < this.fileCache.size(); index++) {
			String line = this.fileCache.get(index);
			if (line.contains("\"RANDOM STATE\"")) {
				headerIndex = index;
				break;
			}
		}

		if (headerIndex == -1) {		//Site 1
			System.out.println("NO RANDOM STATE SECTION DETECTED IN SETUP FILE");	
			return new ArrayList<Integer>();
		}

		String[] stringInts = this.fileCache.get(headerIndex + 1).split(" ");

		for (String textValue : stringInts) {
			textValue = textValue.replace("\"", "");
			textValue = textValue.replace(".", "");
			if (textValue.equals("false")) {
				textValue = "0";
			}
			state.add(Integer.parseInt(textValue));
		}

		return state;
	}

	public String[] getGlobals() {
		int headerIndex = -1;
		// Find the area that follows "Random state" header line
		for (int index = 0; index < this.fileCache.size(); index++) {
			String line = this.fileCache.get(index);
			if (line.contains("GLOBALS")) {
				headerIndex = index;
				break;
			}
		}

		if (headerIndex == -1) {	//Site 2
			System.out.println("ERROR DIDN'T WORK - invalid headerIndex value (-1) - site 2");
			return null;
		}

		return this.fileCache.get(headerIndex + 1).split(cvsSplitBy);
	}

	public List<String[]> getTurtles() {
		List<String[]> birds = new ArrayList<>();

		int headerIndex = 1;
		for (int index = 0; index < this.fileCache.size(); index++) {
			String line = this.fileCache.get(index);
			if (line.contains("\"TURTLES\"")) {
				headerIndex = index;
				break;
			}
		}

		if (headerIndex == -1) {	//Site 3
			System.out.println("ERROR DIDN'T WORK - invalid headerIndex value (-1) - site 3");
			return null;
		}

		int currentRowIndex = headerIndex + 2; // Skip data header row
		try {
			while (this.fileCache.get(currentRowIndex).length() > 5) { // Arbitrary value
				String line = this.fileCache.get(currentRowIndex);
				String[] data = line.split(cvsSplitBy);
				birds.add(data);
				currentRowIndex++;
			}
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Reached end of list");
		}

		return birds;
	}

	public void addHeadingToAgent(List<Agent> birds, int agentId, double reading) {
		int index = 0;
		//TODO: Calculate correct index number
		for(int i = 0 ; i < birds.size() ; i++) {
			Agent currentAgent = birds.get(i);
			if(currentAgent.who == agentId) {
				birds.get(i).addHeading(reading);
				return;
			}
		}
	}

	public void getHeadings(List<Agent> birds, String file) {
		int index;
		double heading;

		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		List<String> testInputRows = this.getRowsFromFile(file);

		for (String line : testInputRows) {
			if( line.startsWith("#")) {
				continue;
			}
			line = line.stripLeading();

			String[] terms = line.split(" ");
			if(terms.length < 2) {
				continue;
			}

			index = Integer.parseInt(terms[0]);
			heading = Double.parseDouble(terms[1]);
			this.addHeadingToAgent(birds, index, heading);
		}
	}
}