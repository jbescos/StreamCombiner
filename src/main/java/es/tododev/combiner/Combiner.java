package es.tododev.combiner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Combiner<ID,E> implements Receiver {

	private final static Logger log = LogManager.getLogger();
	private final ElementUtils<ID,E> elementUtils;
	private final Map<ID,E> combined;
	private final int capacity;
	private final Map<Sender,Boolean> senders = new HashMap<>();
	
	public Combiner(ElementUtils<ID,E> elementUtils, int capacity) {
		this.elementUtils = elementUtils;
		this.capacity = capacity;
		this.combined = new TreeMap<>(elementUtils.comparator());
	}
	
	public synchronized void register(Sender sender){
		senders.put(sender, false);
	}

	@Override
	public void send(Sender sender, String message) throws Exception {
		E newElement = elementUtils.createFromString(message);
		ID id = elementUtils.getID(newElement);
		synchronized (this) {
			E original = combined.get(id);
			if(original == null){
				combined.put(id, newElement);
			}else{
				elementUtils.merge(original, newElement);
			}
			senders.put(sender, true);
			if(isMomentToOutPut()){
				flush();
			}
		}
	}
	
	private boolean isMomentToOutPut(){
		Set<Sender> lateSenders = senders.entrySet().stream().filter(entry -> entry.getValue() == false).map(entry -> entry.getKey()).collect(Collectors.toSet());
		if(lateSenders.size() == 0){
			return true;
		}else if(combined.size() == capacity){
			lateSenders.stream().forEach(sender -> {
				senders.remove(sender);
				sender.timeout();
			});
			return true;
		}else{
			return false;
		}
	}
	
	private void flush(){
		// TODO
	}

}
