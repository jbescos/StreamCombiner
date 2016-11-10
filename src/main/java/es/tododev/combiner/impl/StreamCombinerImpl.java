package es.tododev.combiner.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.tododev.combiner.api.ElementManager;
import es.tododev.combiner.api.OutputWriter;
import es.tododev.combiner.api.Sender;
import es.tododev.combiner.api.StreamCombiner;
import es.tododev.combiner.api.exceptions.ElementSerializerException;
import es.tododev.combiner.api.exceptions.OutputException;
import es.tododev.combiner.api.exceptions.StreamCombinerException;
import es.tododev.combiner.api.exceptions.UnregisteredException;

public class StreamCombinerImpl<ID,E> implements StreamCombiner {

	private final static Logger log = LogManager.getLogger();
	private final ElementManager<ID,E> elementMgr;
	private final OutputWriter<E> writer;
	private final Map<ID,Slot> combined;
	private final int toleranceLimit;
	private final Set<Sender> senders = new HashSet<>();
	private ID latest;
	
	public StreamCombinerImpl(ElementManager<ID,E> elementMgr, int toleranceLimit, OutputWriter<E> writer) {
		this.elementMgr = elementMgr;
		this.toleranceLimit = toleranceLimit;
		this.combined = new TreeMap<>(elementMgr.comparator());
		this.writer = writer;
	}
	
	@Override
	public void register(Sender sender){
		senders.add(sender);
	}
	
	@Override
	public synchronized void unregister(Sender sender){
		log.info("Unregister: "+sender);
		senders.remove(sender);
		combined.values().stream().forEach(slot -> slot.getSenders().remove(sender));
		if(senders.size() == 0){
			try {
				flush(null);
			} catch (ElementSerializerException | OutputException e) {
				log.error("Unexpected error", e);
			}
		}
	}
	
	private void validateEntry(ID newId, Sender sender) throws StreamCombinerException, UnregisteredException{
		if(!senders.contains(sender)){
			throw new UnregisteredException("Sender is not registered");
		}
		if(latest != null){
			int result = elementMgr.comparator().compare(latest, newId);
			if(result != -1){
				throw new StreamCombinerException(newId+" has been already proccessed.");
			}
		}
	}

	@Override
	public void send(Sender sender, String message) throws StreamCombinerException, ElementSerializerException, OutputException, UnregisteredException {
		E newElement = elementMgr.createFromString(message);
		ID id = elementMgr.getID(newElement);
		synchronized (this) {
			log.debug("Size of list: "+combined.size());
			validateEntry(id, sender);
			insertOrMerge(id, newElement, sender);
			ID sendId = getSendId();
			if(sendId != null){
				flush(sendId);
			}
			log.debug("Contains: "+combined.keySet());
		}
	}
	
	private void insertOrMerge(ID newId, E newElement, Sender sender){
		Slot original = combined.get(newId);
		if(original == null){
			original = new Slot(newElement);
			original.init(sender);
			combined.put(newId, original);
		}else{
			original.increment(sender);
			elementMgr.merge(original.getElement(), newElement);
		}
	}
	
	private void sumCounts(Map<Sender,Integer> totalCounts, Map<Sender,Integer> slotCounts){
		for(Entry<Sender,Integer> entry : slotCounts.entrySet()){
			Integer totalCount = totalCounts.get(entry.getKey());
			if(totalCount == null){
				totalCount = 1;
			}else{
				totalCount = totalCount + entry.getValue();
			}
			totalCounts.put(entry.getKey(), totalCount);
		}
	}
	
	private ID getSendId(){
		Map<Sender,Integer> totalCounts = new LinkedHashMap<>();
		List<Entry<ID, Slot>> reversed = new ArrayList<>(combined.entrySet());
		Collections.reverse(reversed);
		for(Entry<ID,Slot> entry : reversed){
			Slot slot = entry.getValue();
			sumCounts(totalCounts, slot.getSenders());
			if(isAllSendersSentTwoTimes(totalCounts)){
				log.debug("ID to deliver: "+entry.getKey());
				return entry.getKey();
			}
		}
		if(combined.size() >= toleranceLimit){
			log.info("Reached the tolerance limit, one sender is going to be unregistered");
			List<Sender> alreadyFound = new ArrayList<>(totalCounts.keySet());
			Collections.reverse(alreadyFound);
			Iterator<Sender> iter = alreadyFound.iterator();
			if(iter.hasNext()){
				unregister(iter.next());
			}
			return getSendId();
		}
		return null;
	}
	
	private boolean isAllSendersSentTwoTimes(Map<Sender,Integer> counts){
		final int TWO_TIMES = 2;
		if(senders.size() == counts.size()){
			for(Integer count : counts.values()){
				if(count < TWO_TIMES){
					return false;
				}
			}
			log.debug("Number of insertions per sender: "+counts);
			return true;
		}
		return false;
	}
	
	private void flush(ID sendId) throws ElementSerializerException, OutputException {	
		List<ID> ordered = new ArrayList<>(combined.keySet());
		log.debug("Flushing till ID "+sendId+" in collection "+ordered);
		for(ID id : ordered){
			latest = id;
			Slot slot = combined.get(id);
			writer.write(slot.getElement());
			combined.remove(id);
			if(sendId != null && sendId == id){
				break;
			}
		}
	}
	
	private final class Slot {
		private final E element;
		private final Map<Sender, Integer> sendersCount = new HashMap<>();
		private Slot(E element){
			this.element = element;
		}
		public E getElement() {
			return element;
		}
		public Map<Sender, Integer> getSenders() {
			return sendersCount;
		}
		public void init(Sender sender){
			sendersCount.put(sender, 1);
		}
		public void increment(Sender sender){
			Integer count = sendersCount.get(sender);
			if(count == null){
				init(sender);
			}else{
				count++;
				sendersCount.put(sender, count);
			}
		}
	}

}
