package es.tododev.combiner;

public interface Receiver {

	void send(Sender sender, String message) throws Exception;
	
}
