import java.util.*;

public class Candidate{
	List<Integer> agents;
	double dci;

	Candidate(List<Integer> agents, double dci){
		this.agents = agents;
		this.dci = dci;
	}

	List<Integer> getAgents(){
		return agents;
	}

	double getDci(){
		return dci;
	}
}