package es.tododev.combiner;

public interface DtoConverter<T> {

	T createFromString(String raw);
	String createFromObj(T obj);
	
}
