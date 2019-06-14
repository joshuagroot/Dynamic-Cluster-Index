import java.util.*;

public class CandidateComparison implements Comparator<Candidate>{

	public int compare(Candidate first, Candidate second){
		if(first.getDci() == second.getDci()){
			return 0;
		} if(first.getDci() < second.getDci()){
			return -1;
		}
		return 1;
	}
}