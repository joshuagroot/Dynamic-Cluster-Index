
import java.io.*;
import java.util.ArrayList;

public class OpenModel {
	private String name;
	private String type;
	private String line = "";
	private BufferedReader br = null;
	private String cvsSplitBy = ",";
	private ArrayList<String> fileCache = new ArrayList<String>();

	private ArrayList<String> getRowsFromFile(String filename) {
		ArrayList<String> results = new ArrayList<String>();

		try {
			br = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			while ((line = br.readLine()) != null) {
				// System.out.println(line);

				results.add(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

	public OpenModel(String name, String type) {
		this.name = name;
		this.type = type;
		this.fileCache = getRowsFromFile(this.name);
	}

	public ArrayList<Integer> getState() {
		ArrayList<Integer> state = new ArrayList<Integer>();

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

	public ArrayList<String[]> getTurtles() {
		ArrayList<String[]> birds = new ArrayList<String[]>();

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

	public void getHeadings(ArrayList<FlockBird> birds, String file) {
		int index;
		double heading;

		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		ArrayList<String> testInputRows = this.getRowsFromFile(file);

		for (String line : testInputRows) {
			line = line.stripLeading();

			String[] terms = line.split(" ");

			index = Integer.parseInt(terms[0]);
			heading = Double.parseDouble(terms[1]);
			birds.get(index).addHeading(heading);
		}
	}
}