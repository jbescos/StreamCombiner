package es.tododev.combiner.api;

@SuppressWarnings("serial")
public class OutputException extends Exception {

	public OutputException(String message, Throwable cause) {
		super(message, cause);
	}

	public OutputException(String message) {
		super(message);
	}

}
