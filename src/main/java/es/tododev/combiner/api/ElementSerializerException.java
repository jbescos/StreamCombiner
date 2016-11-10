package es.tododev.combiner.api;

@SuppressWarnings("serial")
public class ElementSerializerException extends Exception {

	public ElementSerializerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ElementSerializerException(String message) {
		super(message);
	}

}
