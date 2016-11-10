package es.tododev.combiner.api.exceptions;

@SuppressWarnings("serial")
public class ElementSerializerException extends StreamCombinerException {

	public ElementSerializerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ElementSerializerException(String message) {
		super(message);
	}

}
