package es.tododev.combiner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.junit.Before;
import org.junit.Test;

import es.tododev.combiner.dto.Dto;

public class CombinerTest implements Observer {

	private final ElementUtilsImpl utils = new ElementUtilsImpl(new CachedComparator(1000,10));
	private final List<String> output = new ArrayList<>(); 
	
	@Before
	public void before(){
		output.clear();
	}
	
	@Test
	public void exampleFlow() throws Exception{
		Combiner<Long,Dto> combiner = new Combiner<>(utils, 10);
		combiner.addObserver(this);
		Sender sender1 = mock(Sender.class);
		Sender sender2 = mock(Sender.class);
		combiner.register(sender1);
		combiner.register(sender2);
		combiner.send(sender1, "<data> <timestamp>123456789</timestamp> <amount>12</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456789</timestamp> <amount>35</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456790</timestamp> <amount>-20</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>123456791</timestamp> <amount>2</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456790</timestamp> <amount>3</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>123456791</timestamp> <amount>7</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>123456793</timestamp> <amount>10</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456793</timestamp> <amount>9</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456796</timestamp> <amount>39</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>123456799</timestamp> <amount>-89</amount> </data>");
		assertEquals(Arrays.asList(
				"{\"data\":{\"amount\":47.0,\"timestamp\":123456789}}", 
				"{\"data\":{\"amount\":-17.0,\"timestamp\":123456790}}", 
				"{\"data\":{\"amount\":9.0,\"timestamp\":123456791}}", 
				"{\"data\":{\"amount\":19.0,\"timestamp\":123456793}}", 
				"{\"data\":{\"amount\":39.0,\"timestamp\":123456796}}"), output);
	}
	
	@Test
	public void hangedInput(){
		Combiner<Long,Dto> combiner = new Combiner<>(utils, 10);
	}

	@Override
	public void update(Observable o, Object arg) {
		output.add((String)arg);
	}
	
}
