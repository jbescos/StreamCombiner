package es.tododev.combiner.api;

public interface StreamCombiner {

	void register(Sender sender);
	void unregister(Sender sender);
	void send(Sender sender, String message) throws StreamCombinerException;
	
}
