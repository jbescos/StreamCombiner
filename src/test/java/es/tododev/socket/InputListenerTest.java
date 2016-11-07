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
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import es.tododev.combiner.impl.CachedComparator;
import es.tododev.combiner.impl.Combiner;
import es.tododev.combiner.impl.Dto;
import es.tododev.combiner.impl.ElementManagerImpl;

public class InputListenerTest implements Observer {

	private final static Logger log = LogManager.getLogger();
	private final List<String> output = Collections.synchronizedList(new ArrayList<>());
	private final ElementManagerImpl elementMgr = new ElementManagerImpl(new CachedComparator(1000, 100));
	private final int PORT = 25555;
	
	@Before
	public void before(){
		output.clear();
	}
	
	@Test
	public void example() throws InterruptedException, ExecutionException, IOException {
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch listening = new CountDownLatch(1);
		Combiner<Long,Dto> combiner = new Combiner<>(elementMgr, 100);
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
		}));
		
		Future<?> execution = Executors.newSingleThreadExecutor().submit(() -> listener.start());
		start.await();
		Socket socket = new Socket("localhost", PORT);
		listening.await();
		listener.writeInSocket(socket, "<data> <timestamp>123456789</timestamp> <amount>12</amount> </data>\n");
		listener.writeInSocket(socket, "<data> <timestamp>123456789</timestamp> <amount>13</amount> </data>\n");
		listener.writeInSocket(socket, "<data> <timestamp>123456790</timestamp> <amount>12</amount> </data>\n");
		listener.writeInSocket(socket, InputListener.STOP_APPLICATION);
		socket.close();
		execution.get();
		assertEquals(Arrays.asList(
				"{\"data\":{\"amount\":24.0,\"timestamp\":123456789}}",
				"{\"data\":{\"amount\":12.0,\"timestamp\":123456790}}"), output);
	}

	@Override
	public void update(Observable o, Object arg) {
		output.add((String)arg);
	}
	
}
