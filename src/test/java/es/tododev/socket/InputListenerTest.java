package es.tododev.socket;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import es.tododev.combiner.impl.CachedComparator;
import es.tododev.combiner.impl.Dto;
import es.tododev.combiner.impl.ElementManagerImpl;
import es.tododev.combiner.impl.StreamCombinerImpl;
import es.tododev.combiner.impl.StreamCombinerImplTest;
import es.tododev.utils.ConcurrentUtils;
import es.tododev.utils.ConcurrentUtils.ConcurrentTest;

public class InputListenerTest implements Observer {

	private final static Logger log = LogManager.getLogger();
	private final List<String> output = Collections.synchronizedList(new ArrayList<>());
	private final AtomicLong exceptions = new AtomicLong(0);
	private final ElementManagerImpl elementMgr = new ElementManagerImpl(new CachedComparator(1000, 100));
	private final int PORT = 25555;
	
	@Before
	public void before(){
		output.clear();
		exceptions.set(0);
	}
	
	@Test
	@Ignore
	public void example() throws InterruptedException, ExecutionException, IOException, TimeoutException {
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch listening = new CountDownLatch(1);
		final CountDownLatch finish = new CountDownLatch(1);
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 100);
		combiner.addObserver(this);
		InputListener listener = InputListener.createInputListener(1, PORT, combiner, Optional.of(new Visitor(){
			@Override
			public void start() {
				start.countDown();
			}
			@Override
			public void listening() {
				listening.countDown();
			}
			@Override
			public void finish() {
				finish.countDown();
			}
		}));
		
		Executors.newSingleThreadExecutor().submit(() -> listener.start());
		start.await();
		Socket socket = new Socket("localhost", PORT);
		listening.await();
		listener.writeInSocket(socket, 
				"<data> <timestamp>123456789</timestamp> <amount>12</amount> </data>\n"
				+ "<data> <timestamp>123456789</timestamp> <amount>13</amount> </data>\n");
		listener.writeInSocket(socket, "<data> <timestamp>123456790</timestamp> <amount>12</amount> </data>\n");
		listener.writeInSocket(socket, "<data> <timestamp>123456790</timestamp> <amount>12</amount> </data>\n");
		listener.writeInSocket(socket, "<data> <timestamp>123456791</timestamp> <amount>12</amount> </data>\n");
		listener.writeInSocket(socket, InputListener.STOP_APPLICATION+"\n");
		finish.await(10000, TimeUnit.MILLISECONDS);
		socket.close();
		assertEquals(Arrays.asList(
				"{\"data\":{\"amount\":25.0,\"timestamp\":123456789}}",
				"{\"data\":{\"amount\":24.0,\"timestamp\":123456790}}",
				"{\"data\":{\"amount\":12.0,\"timestamp\":123456791}}"), output);
	}
	
	@Test
	@Ignore
	public void inConcurrence() throws Exception{
		log.debug("inConcurrence begin");
		inConcurrence(4, 50);
	}
	
	private void inConcurrence(final int sockets, final int requestsPerThread) throws Exception{
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch listening = new CountDownLatch(sockets);
		final CountDownLatch finish = new CountDownLatch(1);
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 1000);
		combiner.addObserver(this);
		final InputListener listener = InputListener.createInputListener(sockets, PORT, combiner, Optional.of(new Visitor(){
			@Override
			public void start() {
				start.countDown();
			}
			@Override
			public void listening() {
				listening.countDown();
			}
			@Override
			public void finish() {
				finish.countDown();
			}
		}));
		Executors.newSingleThreadExecutor().submit(() -> listener.start());
		start.await();
		ConcurrentUtils.run(sockets, new ConcurrentTest<Socket>() {
			@Override
			public Socket before() throws Exception {
				Socket socket = new Socket("localhost", PORT);
				listening.await();
				return socket;
			}
			@Override
			public void execute(Socket socket) throws Exception {
				for(int i=0; i<requestsPerThread; i++){
					listener.writeInSocket(socket, "<data> <timestamp>"+System.currentTimeMillis()+"</timestamp> <amount>1</amount> </data>\n");
				}
			}
			@Override
			public void onException(Exception e) {
				exceptions.incrementAndGet();
			}
			@Override
			public void lastAction() throws Exception {
				Socket socket = new Socket("localhost", PORT);
				listener.writeInSocket(socket, InputListener.STOP_APPLICATION+"\n");
			}
			@Override
			public void after(Socket socket) throws Exception {
				listener.writeInSocket(socket, InputListener.CLOSE_CONNECTION+"\n");
			}
		});
		finish.await(30000, TimeUnit.MILLISECONDS);
		StreamCombinerImplTest.checkconcurrentTest(output, exceptions.get(), sockets*requestsPerThread);
	}

	@Override
	public void update(Observable o, Object arg) {
		output.add((String)arg);
	}
	
}
