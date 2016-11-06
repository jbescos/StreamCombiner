package es.tododev.combiner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import es.tododev.combiner.dto.Dto;

public class ElementUtilsImplTest {

	@Test
	public void stringConversions() throws JAXBException, IOException{
		ElementUtilsImpl utils = new ElementUtilsImpl((x, y) -> Long.compare(x, y));
		// FIXME?? in the example is </timeStamp> instead of </timestamp>
		Dto dto = utils.createFromString("<data> <timestamp>123456789</timestamp> <amount>1234.567890</amount> </data>");
		String json = utils.createFromObj(dto);
		// FIXME?? in the example of the homework is { "data": { "timestamp":123456789, "amount":"1234.567890" }} instead of {"data":{"amount":1234.56789,"timestamp":123456789}}
		assertEquals("{\"data\":{\"amount\":1234.56789,\"timestamp\":123456789}}", json);
	}
	
	@Test
	public void comparator(){
		CachedComparator comparator = new CachedComparator(6, 3);
		assertEquals(1, comparator.compare(5L, 1L));
		assertEquals(2, comparator.getCacheSize());
		assertEquals(1, comparator.compare(5L, 1L));
		assertEquals(2, comparator.getCacheSize());
		assertEquals(-1, comparator.compare(1L, 5L));
		assertEquals(2, comparator.getCacheSize());
		assertEquals(0, comparator.compare(1L, 1L));
		assertEquals(3, comparator.getCacheSize());
		assertEquals(0, comparator.compare(1L, 1L));
		assertEquals(3, comparator.getCacheSize());
		assertEquals(1, comparator.compare(6L, 1L));
		assertEquals(5, comparator.getCacheSize());
		assertEquals(-1, comparator.compare(1L, 6L));
		assertEquals(5, comparator.getCacheSize());
		assertEquals(-1, comparator.compare(7L, 9L));
		assertEquals(4, comparator.getCacheSize());
	}
	
}
