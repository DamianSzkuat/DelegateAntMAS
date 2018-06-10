package project.exceptions;

public class PheromoneExpiredException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	String str1;
	
	public PheromoneExpiredException(String e){
		str1 = e;
	}
	
	public String toString() {
		return ("PheromoneExpiredException occured: " + str1);
	}

}
