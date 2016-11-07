package es.tododev.combiner.api;

import java.util.Comparator;

public interface ElementManager<ID,E> {

	E createFromString(String raw) throws Exception;
	String createFromObj(E obj) throws Exception;
	ID getID(E obj);
	void merge(E original, E newElement);
	Comparator<ID> comparator();
	
}
