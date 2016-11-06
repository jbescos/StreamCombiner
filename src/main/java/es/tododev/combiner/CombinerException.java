package es.tododev.combiner;

@SuppressWarnings("serial")
public class CombinerException extends Exception {

	public CombinerException(String message) {
		super(message);
	}

	public CombinerException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
