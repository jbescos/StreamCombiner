package es.tododev.combiner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import es.tododev.combiner.dto.Dto;

public class CombinerTest implements Observer {

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
	private final List<String> output = Collections.synchronizedList(new ArrayList<>()); 
	private final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
	private final AtomicLong expensiveCompareInvokation = new AtomicLong(0);
	private final AtomicLong compareInvokation = new AtomicLong(0);
	private final static Logger log = LogManager.getLogger();
	
	@Before
	public void before(){
		output.clear();
		exceptions.clear();
		expensiveCompareInvokation.set(0);
		compareInvokation.set(0);
	}
	
	@Test
	public void exampleFlow() throws CombinerException{
		Combiner<Long,Dto> combiner = new Combiner<>(elementMgr, 10);
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
				"{\"data\":{\"amount\":19.0,\"timestamp\":123456793}}"), output);
		combiner.unregister(sender1);
		combiner.unregister(sender2);
		assertEquals(Arrays.asList(
				"{\"data\":{\"amount\":47.0,\"timestamp\":123456789}}", 
				"{\"data\":{\"amount\":-17.0,\"timestamp\":123456790}}", 
				"{\"data\":{\"amount\":9.0,\"timestamp\":123456791}}", 
				"{\"data\":{\"amount\":19.0,\"timestamp\":123456793}}",
				"{\"data\":{\"amount\":39.0,\"timestamp\":123456796}}",
				"{\"data\":{\"amount\":-89.0,\"timestamp\":123456799}}"), output);
	}
	
	@Test
	public void hangedInput1() throws CombinerException{
		Combiner<Long,Dto> combiner = new Combiner<>(elementMgr, 5);
		combiner.addObserver(this);
		Sender sender1 = mock(Sender.class);
		Sender sender2 = mock(Sender.class);
		Sender sender3 = mock(Sender.class);
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
	
	@Test(expected = CombinerException.class)
	public void wrongInput() throws CombinerException{
		Combiner<Long,Dto> combiner = new Combiner<>(elementMgr, 5);
		combiner.addObserver(this);
		Sender sender1 = mock(Sender.class);
		combiner.register(sender1);
		combiner.send(sender1, "hjdew h239ds");
	}
	
	@Test(expected = CombinerException.class)
	public void alreadyProcessedTimestamp() throws CombinerException{
		Combiner<Long,Dto> combiner = new Combiner<>(elementMgr, 10);
		combiner.addObserver(this);
		Sender sender1 = mock(Sender.class);
		combiner.register(sender1);
		combiner.send(sender1, "<data> <timestamp>123456789</timestamp> <amount>12</amount> </data>");
		combiner.send(sender1, "<data> <timestamp>123456788</timestamp> <amount>35</amount> </data>");
	}
	
	@Test
	public void concurrence() throws InterruptedException{
		inConcurrence(100);
	}
	
	private void inConcurrence(final int requestsPerThread) throws InterruptedException{
		final Combiner<Long,Dto> combiner = new Combiner<>(elementMgr, 10000);
		combiner.addObserver(this);
		final int THREADS = 50;
		ExecutorService service = Executors.newFixedThreadPool(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch end = new CountDownLatch(THREADS);
		for(int i=0; i<THREADS; i++){
			service.execute(() -> {
				try {
					Sender sender = mock(Sender.class);
					combiner.register(sender);
					start.await();
					for(int j=0; j<requestsPerThread;j++){
						combiner.send(sender, "<data> <timestamp>"+System.currentTimeMillis()+"</timestamp> <amount>"+j+"</amount> </data>");
					}
					combiner.unregister(sender);
				} catch (Exception e) {
					exceptions.add(e);
				}
				end.countDown();
			});
		}
		start.countDown();
		end.await();
		assertEquals(0, exceptions.size());
		log.info("Expensive comparations = "+expensiveCompareInvokation.get()+" of total = "+compareInvokation.get());
	}

	@Override
	public void update(Observable o, Object arg) {
		output.add((String)arg);
	}
	
}
