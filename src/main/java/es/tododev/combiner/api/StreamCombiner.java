package es.tododev.combiner.api;

import es.tododev.combiner.api.exceptions.StreamCombinerException;
import es.tododev.combiner.api.exceptions.UnregisteredException;

public interface StreamCombiner {

	void register(Sender sender);
	void unregister(Sender sender);
	void send(Sender sender, String message) throws StreamCombinerException, UnregisteredException;
	
}
