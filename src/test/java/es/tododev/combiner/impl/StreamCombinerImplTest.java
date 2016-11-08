package es.tododev.combiner.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;

import es.tododev.combiner.api.Sender;
import es.tododev.combiner.api.StreamCombinerException;
import es.tododev.utils.ConcurrentUtils;
import es.tododev.utils.ConcurrentUtils.ConcurrentTest;

public class StreamCombinerImplTest implements Observer {

	private final List<String> output = Collections.synchronizedList(new ArrayList<>()); 
	private final AtomicLong exceptions = new AtomicLong(0);
	private final AtomicLong expensiveCompareInvokation = new AtomicLong(0);
	private final AtomicLong compareInvokation = new AtomicLong(0);
	private final static Logger log = LogManager.getLogger();
	private final static ObjectMapper mapper = new ObjectMapper();
	private final ElementManagerImpl elementMgr = new ElementManagerImpl(new CachedComparator(1000, 100){
		@Override
		protected int compareLongs(Long o1, Long o2) {
			expensiveCompareInvokation.incrementAndGet();
			return super.compareLongs(o1, o2);
		}

		@Override
		public int compare(Long o1, Long o2) {
			compareInvokation.incrementAndGet();
			return super.compare(o1, o2);
		}
		
	});
	
	@Before
	public void before(){
		output.clear();
		exceptions.set(0);
		expensiveCompareInvokation.set(0);
		compareInvokation.set(0);
	}
	
	@Test
	public void exampleFlow() throws StreamCombinerException{
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 10);
		combiner.addObserver(this);
		Sender sender1 = new Sender();
		Sender sender2 = new Sender();
		combiner.register(sender1);
		combiner.register(sender2);
		combiner.send(sender1, "<data> <timestamp>123456789</timestamp> <amount>12</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456789</timestamp> <amount>35.5</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456790</timestamp> <amount>-20</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>123456791</timestamp> <amount>2</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456790</timestamp> <amount>3</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>123456791</timestamp> <amount>7</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>123456793</timestamp> <amount>10</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456793</timestamp> <amount>9</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456796</timestamp> <amount>39</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>123456799</timestamp> <amount>-89</amount> </data>");
		assertEquals(Arrays.asList(
				"{\"data\":{\"amount\":47.5,\"timestamp\":123456789}}", 
				"{\"data\":{\"amount\":-17.0,\"timestamp\":123456790}}", 
				"{\"data\":{\"amount\":9.0,\"timestamp\":123456791}}", 
				"{\"data\":{\"amount\":19.0,\"timestamp\":123456793}}"), output);
		combiner.unregister(sender1);
		combiner.unregister(sender2);
		assertEquals(Arrays.asList(
				"{\"data\":{\"amount\":47.5,\"timestamp\":123456789}}", 
				"{\"data\":{\"amount\":-17.0,\"timestamp\":123456790}}", 
				"{\"data\":{\"amount\":9.0,\"timestamp\":123456791}}", 
				"{\"data\":{\"amount\":19.0,\"timestamp\":123456793}}",
				"{\"data\":{\"amount\":39.0,\"timestamp\":123456796}}",
				"{\"data\":{\"amount\":-89.0,\"timestamp\":123456799}}"), output);
	}
	
	@Test
	public void hangedInput1() throws StreamCombinerException{
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 5);
		combiner.addObserver(this);
		Sender sender1 = new Sender();
		Sender sender2 = new Sender();
		Sender sender3 = new Sender();
		combiner.register(sender1);
		combiner.register(sender2);
		combiner.register(sender3);
		combiner.send(sender3, "<data> <timestamp>123456789</timestamp> <amount>5</amount> </data>");
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
				"{\"data\":{\"amount\":52.0,\"timestamp\":123456789}}", 
				"{\"data\":{\"amount\":-17.0,\"timestamp\":123456790}}", 
				"{\"data\":{\"amount\":9.0,\"timestamp\":123456791}}", 
				"{\"data\":{\"amount\":19.0,\"timestamp\":123456793}}"), output);
	}
	
	@Test
	public void unknownIssue() throws StreamCombinerException{
		log.info(" test-> unknownIssue ");
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 100);
		combiner.addObserver(this);
		Sender sender1 = new Sender();
		Sender sender2 = new Sender();
		combiner.register(sender1);
		combiner.register(sender2);
		
		combiner.send(sender2, "<data> <timestamp>1478432883046</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478432883046</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478432883046</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478432883046</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478432883047</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478432883047</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478432883047</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478432883047</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478432883047</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478432883048</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478432883048</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478432883048</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478432883049</timestamp> <amount>1</amount> </data>");
		
		assertEquals(Arrays.asList(
				"{\"data\":{\"amount\":4.0,\"timestamp\":1478432883046}}", 
				"{\"data\":{\"amount\":5.0,\"timestamp\":1478432883047}}"), output);
	}
	
	@Test(expected = StreamCombinerException.class)
	public void wrongInput() throws StreamCombinerException{
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 5);
		combiner.addObserver(this);
		Sender sender1 = new Sender();
		combiner.register(sender1);
		combiner.send(sender1, "hjdew h239ds");
	}
	
	@Test(expected = StreamCombinerException.class)
	public void alreadyProcessedTimestamp() throws StreamCombinerException{
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 10);
		combiner.addObserver(this);
		Sender sender1 = new Sender();
		combiner.register(sender1);
		combiner.send(sender1, "<data> <timestamp>123456789</timestamp> <amount>12</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456788</timestamp> <amount>35</amount> </data>");
	}
	
	@Test
	public void startWithSameTimestamp() throws StreamCombinerException{
		log.debug(" test -> startWithSameTimestamp");
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 100);
		combiner.addObserver(this);
		Sender sender1 = new Sender();
		Sender sender2 = new Sender();
		combiner.register(sender1);
		combiner.register(sender2);
		combiner.send(sender1, "<data> <timestamp>1478444952989</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478444952990</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478444952989</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478444952990</timestamp> <amount>1</amount> </data>");
		assertEquals(Arrays.asList("{\"data\":{\"amount\":2.0,\"timestamp\":1478444952989}}"), output);
	}
	
	@Test
//	@Ignore
	public void concurrence() throws Exception{
		inConcurrence(500, 5);
	}
	
	private void inConcurrence(final int requestsPerThread, final int threads) throws Exception{
		final StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 10000);
		combiner.addObserver(this);
		ConcurrentUtils.run(threads, new ConcurrentTest<Sender>() {
			@Override
			public Sender before() throws Exception {
				Sender sender = new Sender();
				combiner.register(sender);
				return sender;
			}
			@Override
			public void execute(Sender sender) throws Exception {
				for(int j=0; j<requestsPerThread;j++){
					combiner.send(sender, "<data> <timestamp>"+System.currentTimeMillis()+"</timestamp> <amount>1</amount> </data>");
				}
			}
			@Override
			public void onException(Exception e) {
				exceptions.incrementAndGet();
				log.error("Exception", e);
			}
			@Override
			public void lastAction() throws Exception {}
			@Override
			public void after(Sender sender) throws Exception {
				combiner.unregister(sender);
			}
		});
		checkconcurrentTest(output, exceptions.get(), requestsPerThread*threads);
		log.info("Expensive comparations = "+expensiveCompareInvokation.get()+" of total = "+compareInvokation.get());
	}

	@Override
	public void update(Observable o, Object arg) {
		output.add((String)arg);
	}
	
	public static void checkconcurrentTest(List<String> output, long exceptions, int totalAmount){
		int amount = 0;
		Dto previous = null;
		for(int i=0;i<output.size();i++){
			Dto current = jsonToDto(output.get(i));
			amount = (int) (amount + current.getAmount());
			if(previous != null){
				assertEquals("Timestamps error in index = "+i+", previous = "+previous+" and current = "+current, -1, Long.compare(previous.getTimestamp(), current.getTimestamp()));
			}
			previous = current;
		}
		assertEquals(totalAmount, amount);
		assertEquals(0, exceptions);
		log.debug(output);
	}
	
	private static Dto jsonToDto(String json){
		try{
			Map<String, Map<String, Object>> value =  mapper.readValue(json, new TypeReference<Map<String, Map<String, Object>>>() {});
			Map<String, Object> content = value.get("data");
			Dto dto = new Dto();
			dto.setTimestamp((Long)content.get("timestamp"));
			dto.setAmount((Double)content.get("amount"));
			return dto;
		} catch (IOException e) {
			log.error("Can not parse JSON "+json, e);
			throw new IllegalArgumentException("Can not parse JSON "+json, e);
		}
	}
	
}