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
	private final ElementManager<ID,E> elementMgr;
	private final Map<ID,E> combined;
	private final int toleranceLimit;
	private final Map<Sender,SenderInfo> senders = new HashMap<>();
	
	public Combiner(ElementManager<ID,E> elementMgr, int toleranceLimit) {
		this.elementMgr = elementMgr;
		this.toleranceLimit = toleranceLimit;
		this.combined = new TreeMap<>(elementMgr.comparator());
	}
	
	public synchronized void register(Sender sender){
		senders.put(sender, new SenderInfo(new ArrayList<>()));
	}
	
	public synchronized void unregister(Sender sender){
		senders.remove(sender);
		sender.timeout();
		if(senders.size() == 0){
			try {
				flush(combined.size()-1);
			} catch (Exception e) {
				log.error("Unexpected error", e);
			}
		}
	}

	@Override
	public void send(Sender sender, String message) throws CombinerException {
		try{
			E newElement = elementMgr.createFromString(message);
			ID id = elementMgr.getID(newElement);
			synchronized (this) {
				log.debug("Sender "+sender+": "+id);
				if(!senders.containsKey(sender)){
					throw new IllegalStateException("Sender is not registered");
				}
				ID cutId = getCutId();
				checkValidId(id, cutId);
				E original = combined.get(id);
				if(original == null){
					combined.put(id, newElement);
				}else{
					elementMgr.merge(original, newElement);
				}
				SenderInfo senderInfo = senders.get(sender);
				if(!senderInfo.processed.contains(id)){
					senderInfo.processed.add(id);
				}
				int sendToIdx = calculateIdxToSend(cutId);
				if(sendToIdx > -1){
					flush(sendToIdx);
				}
			}
		}catch(Exception e){
			log.error("ERROR: "+message, e);
			throw new CombinerException("ERROR: "+message, e);
		}
	}
	
	private void checkValidId(ID id, ID cutId){
		if(cutId != null){
			int result = elementMgr.comparator().compare(cutId, id);
			if(result == 1){
				throw new IllegalArgumentException(id+" has been already proccessed.");
			}
		}
	}
	
	private ID getCutId(){
		ID cutId = senders.values().stream().filter(info -> info.processed.size() > 0).map(info -> info.processed.get(info.processed.size()-1)).reduce(BinaryOperator.minBy(elementMgr.comparator())).orElse(null);
		log.debug("CutId value = "+cutId);
		return cutId;
	}
	
	private int getMinSenderIdx(ID minID){
		List<ID> list = combined.keySet().stream().collect(Collectors.toList());
		int idx = list.indexOf(minID);
		log.debug("Returning idx = "+idx+" of "+list.size()+" elements. Limit value = "+minID);
		for(SenderInfo senderInfo : senders.values()){
			int fromIndex = senderInfo.processed.indexOf(minID);
			if(fromIndex > -1){
				for(int i=0;i<fromIndex+1;i++){
					ID removed = senderInfo.processed.remove(0);
					log.debug("Removed "+removed+" from collection "+senderInfo.processed);
				}
			}
		}
		return idx;
	}
	
	private int calculateIdxToSend(ID cutId){
		if(cutId != null){
			Set<Sender> readyToFlushSenders = senders.entrySet().stream().filter(entry -> entry.getValue().isReadyToFlush(cutId)).map(entry -> entry.getKey()).collect(Collectors.toSet());
			if(readyToFlushSenders.size() == senders.size()){
				log.debug("Having the messages from all the inputs");
				return getMinSenderIdx(cutId);
			}else if(combined.size() >= toleranceLimit){
				List<Sender> notReadyToFlushSenders = senders.keySet().stream().filter(sender -> !readyToFlushSenders.contains(sender)).collect(Collectors.toList());
				log.warn("Reached the tolerance limit, we are going to discard the hanged inputs: "+notReadyToFlushSenders);
				notReadyToFlushSenders.stream().forEach(sender -> unregister(sender));
			}
		}
		return -1;
	}
	
	private void flush(int sendToIdx) throws Exception {	
		List<ID> ordered = new ArrayList<>(combined.keySet());
		log.debug("Flushing from 0 to "+sendToIdx+" in collection "+ordered);
		for(int i=0; i<(sendToIdx+1);i++){
			E element = combined.remove(ordered.get(i));
			String text = elementMgr.createFromObj(element);
			log.debug("Notify "+text);
			setChanged();
			notifyObservers(text);
		}
	}
	
	private class SenderInfo{
		private List<ID> processed;
		private SenderInfo(List<ID> processed){
			this.processed = processed;
		}
		private boolean isReadyToFlush(ID minID){
			for(ID id : processed){
				if(elementMgr.comparator().compare(minID, id) == -1){
					log.debug(id+" higher than "+minID+", this sender is ready to flush");
					return true;
				}
			}
			log.debug("Not ready to flush, "+minID+" is the lowest in "+processed);
			return false;
		}
	}

}
