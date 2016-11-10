package es.tododev.combiner.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import es.tododev.combiner.api.OutputWriter;
import es.tododev.combiner.api.Sender;
import es.tododev.combiner.api.exceptions.ElementSerializerException;
import es.tododev.combiner.api.exceptions.OutputException;
import es.tododev.combiner.api.exceptions.StreamCombinerException;
import es.tododev.combiner.api.exceptions.UnregisteredException;
import es.tododev.utils.ConcurrentUtils;
import es.tododev.utils.ConcurrentUtils.ConcurrentTest;

public class StreamCombinerImplTest implements OutputWriter<Dto> {

	private final List<Dto> output = Collections.synchronizedList(new ArrayList<>()); 
	private final AtomicLong exceptions = new AtomicLong(0);
	private final AtomicLong expensiveCompareInvokation = new AtomicLong(0);
	private final AtomicLong compareInvokation = new AtomicLong(0);
	private final static Logger log = LogManager.getLogger();
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
	public void exampleFlow() throws StreamCombinerException, ElementSerializerException, OutputException, UnregisteredException{
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 10, this);
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
				new Dto(123456789, 47.5),
				new Dto(123456790, -17.0),
				new Dto(123456791, 9.0),
				new Dto(123456793, 19.0)), output);
		combiner.unregister(sender1);
		combiner.unregister(sender2);
		assertEquals(Arrays.asList(
				new Dto(123456789, 47.5),
				new Dto(123456790, -17.0),
				new Dto(123456791, 9.0),
				new Dto(123456793, 19.0),
				new Dto(123456796, 39.0),
				new Dto(123456799, -89.0)), output);
	}
	
	@Test
	public void hangedInput1() throws StreamCombinerException, ElementSerializerException, OutputException, UnregisteredException{
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 5, this);
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
				new Dto(123456789, 52.0),
				new Dto(123456790, -17.0),
				new Dto(123456791, 9.0),
				new Dto(123456793, 19.0)), output);
	}
	
	@Test
	public void unknownIssue() throws StreamCombinerException, ElementSerializerException, OutputException, UnregisteredException{
		log.info(" test-> unknownIssue ");
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 100, this);
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
				new Dto(1478432883046L, 4.0),
				new Dto(1478432883047L, 5.0)), output);
	}
	
	@Test(expected = ElementSerializerException.class)
	public void wrongInput() throws StreamCombinerException, ElementSerializerException, OutputException, UnregisteredException{
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 5, this);
		Sender sender1 = new Sender();
		combiner.register(sender1);
		combiner.send(sender1, "hjdew h239ds");
	}
	
	@Test(expected = StreamCombinerException.class)
	public void alreadyProcessedTimestamp() throws StreamCombinerException, ElementSerializerException, OutputException, UnregisteredException{
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 10, this);
		Sender sender1 = new Sender();
		combiner.register(sender1);
		combiner.send(sender1, "<data> <timestamp>123456789</timestamp> <amount>12</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456788</timestamp> <amount>35</amount> </data>");
		assertEquals(Arrays.asList(new Dto(123456788, 35.0)), output);
		combiner.send(sender1, "<data> <timestamp>123456788</timestamp> <amount>35</amount> </data>");
	}
	
	@Test
	public void startWithSameTimestamp() throws StreamCombinerException, ElementSerializerException, OutputException, UnregisteredException{
		log.debug(" test -> startWithSameTimestamp");
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 100, this);
		Sender sender1 = new Sender();
		Sender sender2 = new Sender();
		combiner.register(sender1);
		combiner.register(sender2);
		combiner.send(sender1, "<data> <timestamp>1478444952989</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478444952990</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478444952989</timestamp> <amount>1</amount> </data>");
		combiner.send(sender2, "<data> <timestamp>1478444952990</timestamp> <amount>1</amount> </data>");
		assertEquals(Arrays.asList(new Dto(1478444952989L, 2.0)), output);
	}
	
	@Test
	public void outputException() throws StreamCombinerException, ElementSerializerException, OutputException, UnregisteredException{
		OutputWriter<Dto> writer = new OutputWriter<Dto>(){
			private int counter = 0;
			@Override
			public void write(Dto content) throws OutputException, ElementSerializerException {
				if(counter >=1){
					throw new OutputException("Error message");
				}
				StreamCombinerImplTest.this.write(content);
				counter++;
			}
			
		};
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 100, writer);
		Sender sender1 = new Sender();
		combiner.register(sender1);
		combiner.send(sender1, "<data> <timestamp>1478444952989</timestamp> <amount>1</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>1478444952990</timestamp> <amount>1</amount> </data>");
		assertEquals(Arrays.asList(new Dto(1478444952989L, 1.0)), output);
		try{
			combiner.send(sender1, "<data> <timestamp>1478444952991</timestamp> <amount>1</amount> </data>");
			fail("Unreacheable code");
		}catch(OutputException e){
			assertEquals(Arrays.asList(new Dto(1478444952989L, 1.0)), output);
		}
	}
	
	@Test
//	@Ignore
	public void concurrence() throws Exception{
		inConcurrence(500, 5);
	}
	
	private void inConcurrence(final int requestsPerThread, final int threads) throws Exception{
		final StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 10000, this);
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

	public static void checkconcurrentTest(List<Dto> output, long exceptions, int totalAmount){
		int amount = 0;
		Dto previous = null;
		for(int i=0;i<output.size();i++){
			Dto current = output.get(i);
			amount = (int) (amount + current.getAmount());
			if(previous != null){
				assertEquals("Timestamps error in index = "+i+", previous = "+previous+" and current = "+current, -1, Long.compare(previous.getTimestamp(), current.getTimestamp()));
			}
			previous = current;
		}
		assertEquals(totalAmount, amount);
		assertEquals(0, exceptions);
	}
	
	@Override
	public void write(Dto content) throws OutputException, ElementSerializerException {
		output.add(content);
	}
	
}
