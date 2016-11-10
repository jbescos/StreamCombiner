package es.tododev.combiner.api.exceptions;

@SuppressWarnings("serial")
public class OutputException extends StreamCombinerException {

	public OutputException(String message, Throwable cause) {
		super(message, cause);
	}

	public OutputException(String message) {
		super(message);
	}

}
