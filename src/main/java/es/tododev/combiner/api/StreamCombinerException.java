package es.tododev.combiner.api;

@SuppressWarnings("serial")
public class StreamCombinerException extends Exception {

	public StreamCombinerException(String message) {
		super(message);
	}

	public StreamCombinerException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
