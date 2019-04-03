
import java.io.*;
import java.util.ArrayList;

public class OpenModel{
	private String name;
	private String type;
	private String line = "";
	private	BufferedReader br = null;
	private String cvsSplitBy = ",";

	public OpenModel(String name, String type){
		this.name = name;
		this.type = type;
		try{
			br = new BufferedReader(new FileReader(name));
		} catch (FileNotFoundException e){
			e.printStackTrace();
		}
	}

	public ArrayList<Integer> getState(){
		ArrayList<Integer> state = new ArrayList<Integer>();

		try{
			while ((line = br.readLine()) != null){
			//System.out.println(line);

				if (line.equals("\"RANDOM STATE\"")){
					break;
				}
			}

			String[] stringInts = br.readLine().split(" ");

			for (String textValue : stringInts){
				textValue = textValue.replace("\"", "");
				textValue = textValue.replace(".", "");
				if (textValue.equals("false")){
					textValue = "0";
				}
				//System.out.println(textValue);
				state.add(Integer.parseInt(textValue));
			}


		} catch (IOException e){
			e.printStackTrace();
		}
		

		return state;
	}

	public String[] getGlobals(){
		try{
			while ((line = br.readLine()) != null){
			//System.out.println(line);

				if (line.equals("\"GLOBALS\"")){
					break;
				}

			//	System.out.println(br.readLine());

			}
			br.readLine();


			return br.readLine().split(cvsSplitBy);


		} catch (IOException e){
			e.printStackTrace();
		}
		return null;
	}
	public ArrayList<String[]> getTurtles(){
		ArrayList<String[]> birds = new ArrayList<String[]>();

		try {
			System.out.println("Opening file...");

			

			while ((line = br.readLine()) != null){
				if (line.equals("\"TURTLES\"")){
					break;
				}
			}
			System.out.println("Opening file...");

			br.readLine();
			int count = 0;
			while ((line = br.readLine()) != null){
				if (line.equals("")){
					break;
				}

				String[] data = line.split(cvsSplitBy);
				birds.add(data);
			} 
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
        	br.close();
        } catch (IOException e) {
        	e.printStackTrace();
        }
		return birds;
	}

	public void getHeadings(ArrayList<FlockBird> birds, String file){
		int index;
		double heading;

		try{
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e){
			e.printStackTrace();
		}

		try{
			while ((line = br.readLine()) != null){
				index = Integer.parseInt(line.substring(1, line.lastIndexOf(' ')));
				heading = Double.parseDouble(line.substring(line.lastIndexOf(' ') + 1));

				//System.out.println(index + " " + heading);

				birds.get(index).addHeading(heading);
			}

		} catch (IOException e){
			e.printStackTrace();
		}
		try{
        	br.close();
        } catch (IOException e) {
        	e.printStackTrace();
        }

	}

}