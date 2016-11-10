package es.tododev.combiner.api;

import es.tododev.combiner.api.exceptions.ElementSerializerException;
import es.tododev.combiner.api.exceptions.OutputException;

public interface OutputWriter<T> {

	void write(T content) throws OutputException, ElementSerializerException;
	
}
