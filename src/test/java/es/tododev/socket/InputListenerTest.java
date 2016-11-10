package es.tododev.socket;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import es.tododev.combiner.api.exceptions.ElementSerializerException;
import es.tododev.combiner.api.exceptions.OutputException;
import es.tododev.combiner.impl.CachedComparator;
import es.tododev.combiner.impl.Dto;
import es.tododev.combiner.impl.ElementManagerImpl;
import es.tododev.combiner.impl.StreamCombinerImpl;
import es.tododev.utils.ConcurrentUtils;
import es.tododev.utils.ConcurrentUtils.ConcurrentTest;

public class InputListenerTest {

	private final static Logger log = LogManager.getLogger();
	private final ElementManagerImpl elementMgr = new ElementManagerImpl(new CachedComparator(1000, 100));
	private final OutputWriterImpl<Long, Dto> out = new OutputWriterImpl<>("target/output.log", elementMgr);
	private final int PORT = 25555;
	
	@Test
	@Ignore
	public void inConcurrence() throws Exception{
		log.debug("inConcurrence begin");
		inConcurrence(4, 1000);
	}
	
	private void inConcurrence(final int sockets, final int requestsPerThread) throws Exception{
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch listening = new CountDownLatch(sockets);
		final CountDownLatch finish = new CountDownLatch(1);
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 1000, out);
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
			public void onException(Exception e) {}
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
		log.info("Finish test inConcurrence");
	}
	
	@Test
	public void createExampleFiles() throws InterruptedException, ExecutionException, OutputException, ElementSerializerException, IOException{
		final int FILES = 3;
		final int LINES = 5000;
		CompletionService<String> ecs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(FILES));
		for(int i=0;i<FILES;i++){
			ecs.submit(() -> {
				StringBuilder builder = new StringBuilder();
				IntStream.range(0, LINES).forEach(j -> builder.append("<data> <timestamp>"+System.currentTimeMillis()+"</timestamp> <amount>1</amount> </data>\n"));
				return builder.toString();
			});
		}
		
		for(int i=0;i<FILES;i++){
			String text = ecs.take().get()+"\n";
			String fileName = "target/file"+i+".log";
			Files.write(Paths.get(fileName), text.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}	
	}
	
}
