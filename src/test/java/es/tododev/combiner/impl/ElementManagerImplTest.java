package es.tododev.combiner.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

import es.tododev.combiner.api.exceptions.ElementSerializerException;
import es.tododev.combiner.impl.CachedComparator;
import es.tododev.combiner.impl.ElementManagerImpl;

public class ElementManagerImplTest {

	private long expensiveCompare;
	
	@Before
	public void before(){
		expensiveCompare = 0L;
	}
	
	@Test
	public void stringConversions() throws JAXBException, IOException, ElementSerializerException{
		ElementManagerImpl elementMgr = new ElementManagerImpl((x, y) -> Long.compare(x, y));
		// FIXME?? in the example is </timeStamp> instead of </timestamp>
		Dto dto = elementMgr.createFromString("<data> <timestamp>123456789</timestamp> <amount>1234.567890</amount> </data>");
		String json = elementMgr.createFromObj(dto);
		// FIXME?? in the example of the homework is { "data": { "timestamp":123456789, "amount":"1234.567890" }} instead of {"data":{"amount":1234.56789,"timestamp":123456789}}
		assertEquals("{\"data\":{\"amount\":1234.56789,\"timestamp\":123456789}}", json);
	}
	
	@Test
	public void comparator(){
		CachedComparator comparator = new CachedComparator(5, 3){
			@Override
			protected int compareLongs(Long o1, Long o2) {
				expensiveCompare++;
				return super.compareLongs(o1, o2);
			}
		};
		assertEquals(1, comparator.compare(5L, 1L));
		assertEquals(1, expensiveCompare);
		assertEquals(2, comparator.getCacheSize());
		
		assertEquals(1, comparator.compare(5L, 1L));
		assertEquals(1, expensiveCompare);
		assertEquals(2, comparator.getCacheSize());
		
		assertEquals(-1, comparator.compare(1L, 5L));
		assertEquals(1, expensiveCompare);
		assertEquals(2, comparator.getCacheSize());
		
		assertEquals(0, comparator.compare(1L, 1L));
		assertEquals(1, expensiveCompare);
		assertEquals(2, comparator.getCacheSize());
		
		assertEquals(0, comparator.compare(1L, 1L));
		assertEquals(1, expensiveCompare);
		assertEquals(2, comparator.getCacheSize());
		
		assertEquals(1, comparator.compare(6L, 1L));
		assertEquals(2, expensiveCompare);
		assertEquals(4, comparator.getCacheSize());
		
		assertEquals(-1, comparator.compare(1L, 6L));
		assertEquals(2, expensiveCompare);
		assertEquals(4, comparator.getCacheSize());
		
		assertEquals(-1, comparator.compare(7L, 9L));
		assertEquals(3, expensiveCompare);
		assertEquals(3, comparator.getCacheSize());
	}
	
}
