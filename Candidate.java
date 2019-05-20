import java.util.List;
import java.util.Set;
import java.util.HashSet;

import java.io.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public class Candidate{
	private List<Integer> agents;
	private double dci;

	Candidate(List<Integer> agents, double dci){
		this.agents = agents;
		this.dci = dci;
	}
	
	public List<Integer> getAgents(){
		return agents;
	}

	public double getDci(){
		return dci;
	}

	@SuppressWarnings("unchecked")
	public void save(){

		JSONObject candidateList = new JSONObject();
		JSONObject dciValue = new JSONObject();
		StringBuilder candidateString = new StringBuilder();

		for(int i : agents){
			candidateString.append(i + " ");
		}
		candidateList.put("agents",candidateString.toString());
		candidateList.put("DCI",dci);
	
		try (FileWriter file = new FileWriter("savedDCI/" + candidateString.toString() + ".json")) {
 
			file.write(candidateList.toJSONString());
			file.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Set<Integer> getSet(){
		return new HashSet<>(agents);

	}
	public String toString(){
		return agents.toString() + " : " + dci;
	}
}