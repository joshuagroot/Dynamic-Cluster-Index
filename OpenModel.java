
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

		if (headerIndex == -1) {
			System.out.println("ERROR DIDN'T WORK");
			return null;
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
			if (line.contains("\"GLOBALS\"")) {
				headerIndex = index;
				break;
			}
		}

		if (headerIndex == -1) {
			System.out.println("ERROR DIDN'T WORK");
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

		if (headerIndex == -1) {
			System.out.println("ERROR DIDN'T WORK");
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

	public void getHeadings(List<FlockBird> birds, String file) {
		int index;
		double heading;

		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		List<String> testInputRows = this.getRowsFromFile(file);

		for (String line : testInputRows) {
			while(line.charAt(0) == ' '){
				line = line.substring(1);
			}
			//line = line.stripLeading();

			String[] terms = line.split(" ");

			index = Integer.parseInt(terms[0]);
			heading = Double.parseDouble(terms[1]);
			birds.get(index).addHeading(heading);
		}
	}

	public void getFlockMatesAndHeadings(List<FlockBird> birds, String file){
		int index;
		double heading;

		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}


		List<String> testInputRows = this.getRowsFromFile(file);
		List<Integer> currentMates = new ArrayList<>();
		int currentBird = 0;

		for (String line : testInputRows) {
			while(line.charAt(0) == ' '){
				line = line.substring(1);
			}

			if(line.contains("heading:")){
				String[] terms = line.split(" heading: ");
				index = Integer.parseInt(terms[0]);
				heading = Double.parseDouble(terms[1]);
				birds.get(index).addHeading(heading);
			} else if(line.contains(" is mates with:")){
				currentBird = Integer.parseInt(line.substring(0, line.indexOf(" ")));
			} else if(line.contains(" is done")){
				birds.get(currentBird).addFlockMates(currentMates);
				currentMates = new ArrayList<>();
			} else{
				currentMates.add(Integer.parseInt(line));
			}

			String[] terms = line.split(" ");
		}
	}
}