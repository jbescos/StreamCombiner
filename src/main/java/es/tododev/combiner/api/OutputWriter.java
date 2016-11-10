package es.tododev.combiner.api;

public interface OutputWriter<T> {

	void write(T content) throws OutputException, ElementSerializerException;
	
}
