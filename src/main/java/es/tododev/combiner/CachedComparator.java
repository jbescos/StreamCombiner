package es.tododev.combiner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CachedComparator implements Comparator<Long> {

	private final static Logger log = LogManager.getLogger();
	private final Map<Key,Integer> cache = new LinkedHashMap<>();
	private final int cacheSize;
	private final int removeWhenFull;
	
	public CachedComparator(int cacheSize, int removeWhenFull){
		this.cacheSize = cacheSize;
		this.removeWhenFull = removeWhenFull;
	}
	
	@Override
	public int compare(Long o1, Long o2) {
		Key key1 = new Key(o1, o2);
		Integer result = cache.get(key1);
		if(result == null){
			result = Long.compare(o1, o2);
			cache.put(key1, result);
			if(o1 != o2){
				Key key2 = new Key(o2, o1);
				cache.put(key2, result*(-1));
			}
			if(cache.size() > cacheSize){
				Iterator<Key> iter = new ArrayList<>(cache.keySet()).iterator();
				int index = 0;
				while(iter.hasNext() && index < removeWhenFull){
					cache.remove(iter.next());
					index++;
				}
			}
		}
		return result;
	}
	
	int getCacheSize(){
		return cache.size();
	}
	
	private class Key {
		private final long o1;
		private final long o2;
		public Key(long o1, long o2) {
			this.o1 = o1;
			this.o2 = o2;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (o1 ^ (o1 >>> 32));
			result = prime * result + (int) (o2 ^ (o2 >>> 32));
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (o1 != other.o1)
				return false;
			if (o2 != other.o2)
				return false;
			return true;
		}
		private CachedComparator getOuterType() {
			return CachedComparator.this;
		}
		
	}
	

}
