package es.tododev.combiner.api;

import java.util.Comparator;

import es.tododev.combiner.api.exceptions.ElementSerializerException;

public interface ElementManager<ID,E> {

	E createFromString(String raw) throws ElementSerializerException;
	String createFromObj(E obj) throws ElementSerializerException;
	ID getID(E obj);
	void merge(E original, E newElement);
	Comparator<ID> comparator();
	
}
