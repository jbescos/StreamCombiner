package es.tododev.combiner;

public interface Receiver {

	void send(String message) throws Exception;
	
}
