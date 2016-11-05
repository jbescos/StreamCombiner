package es.tododev.combiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Combiner<ID,E> extends Observable implements Receiver {

	private final static Logger log = LogManager.getLogger();
	private final ElementUtils<ID,E> elementUtils;
	private final Map<ID,E> combined;
	private final int capacity;
	private final Map<Sender,SenderInfo> senders = new HashMap<>();
	
	public Combiner(ElementUtils<ID,E> elementUtils, int capacity) {
		this.elementUtils = elementUtils;
		this.capacity = capacity;
		this.combined = new TreeMap<>(elementUtils.comparator());
	}
	
	public synchronized void register(Sender sender){
		senders.put(sender, new SenderInfo(false, null));
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
			SenderInfo senderInfo = senders.get(sender);
			senderInfo.sent = true;
			senderInfo.limit = id;
			int sendToIdx = calculateIdxToSend();
			if(sendToIdx > -1){
				flush(sendToIdx);
			}
		}
	}
	
	private int getMinSenderIdx(){
		ID minID = senders.values().stream().map(info -> info.limit).reduce(BinaryOperator.minBy(elementUtils.comparator())).get();
		SenderInfo min = senders.values().stream().filter(info -> info.limit == minID).findFirst().get();
		List<ID> list = combined.keySet().stream().collect(Collectors.toList());
		int idx = list.indexOf(minID);
		log.debug("Returning idx = "+idx+" of "+list.size()+" elements. Limit value = "+minID);
		min.sent = false;
		return idx;
	}
	
	private int calculateIdxToSend(){
		Set<Sender> lateSenders = senders.entrySet().stream().filter(entry -> entry.getValue().sent == false).map(entry -> entry.getKey()).collect(Collectors.toSet());
		if(lateSenders.size() == 0){
			log.debug("Having the messages from all the inputs");
			return getMinSenderIdx();
		}else if(combined.size() == capacity){
			log.warn("Reached the limit of espace, we are going to close some late connections");
			lateSenders.stream().forEach(sender -> {
				senders.remove(sender);
				sender.timeout();
			});
			return getMinSenderIdx();
		}else{
			return -1;
		}
	}
	
	private void flush(int sendToIdx) throws Exception{	
		List<ID> ordered = new ArrayList<>(combined.keySet());
		log.debug("Flushing from 0 to "+sendToIdx+" in collection "+ordered);
		for(int i=0; i<(sendToIdx+1);i++){
			E element = combined.remove(ordered.get(i));
			String text = elementUtils.createFromObj(element);
			log.debug("Notify "+text);
			setChanged();
			notifyObservers(text);
		}
	}
	
	private class SenderInfo{
		private boolean sent;
		private ID limit;
		private SenderInfo(boolean sent, ID limit){
			this.sent = sent;
			this.limit = limit;
		}
	}

}
