package es.tododev.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.tododev.combiner.api.Sender;
import es.tododev.combiner.api.StreamCombiner;
import es.tododev.combiner.api.StreamCombinerException;

public final class InputListener {

	private final static Logger log = LogManager.getLogger();
	private final int nSockets;
	private final int port;
	private final static int LIMIT_N_SOCKETS = 100;
	private final ExecutorService service;
	private final StreamCombiner streamCombiner;
	private final Optional<Visitor> visitor;
	private AtomicInteger currentSockets = new AtomicInteger(0);
	private final static String NEW_LINE = System.getProperty("line.separator");
	public final static String STOP_APPLICATION = "STOP";
	public final static String CLOSE_CONNECTION = "CLOSE";
	
	private InputListener(int nSockets, int port, StreamCombiner streamCombiner, Optional<Visitor> visitor){
		this.nSockets = nSockets;
		this.port = port;
		this.streamCombiner = streamCombiner;
		this.service = Executors.newFixedThreadPool(nSockets);
		this.visitor = visitor;
	}
	
	public void start() {
		try(ServerSocket serverSocket = new ServerSocket(port)){
			if(visitor.isPresent()){
		    	visitor.get().start();
		    }
			while(!service.isShutdown()){
				if(currentSockets.get() < nSockets){
					log.debug("Listening for new connections");
					Socket socket = serverSocket.accept();
					log.debug("New connection accepted");
					writeInSocket(socket, "Connection accepted: "+socket);
					Sender sender = new Sender();
					streamCombiner.register(sender);
					log.debug("Register the sender in the stream combiner");
					service.execute(() -> handleRequest(socket, sender));
					currentSockets.incrementAndGet();
				}
			}
			log.info("Finish socket reading");
		}catch(IOException e){
			log.error("Error in server socket", e);
			stop();
		}
	}
	
	private void stop() {
		log.debug("Stopping application");
		service.shutdown();
		streamCombiner.stop();
		try {
			Socket lastConnection = new Socket("localhost", port);
			lastConnection.close();
		} catch (IOException e) {
			log.error("Can not send the signal to stop listening the server socket");
		}
		if(visitor.isPresent()){
	    	visitor.get().finish();
	    }
	}
	
	private void disconnect(Socket socket, Sender sender){
		try {
			streamCombiner.unregister(sender);
			log.debug("Closing connection "+socket);
			currentSockets.decrementAndGet();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				log.error("Can not close the connection "+socket+", stopping the application", e);
				stop();
			}
		}
	}
	
	void writeInSocket(Socket socket, String message) {
		try{
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			writer.write(message);
			writer.flush();
		} catch (IOException e) {
			log.error("Can not write response: "+message, e);
		}
	}
	
	private void handleRequest(Socket socket, Sender sender){
		log.debug("Listening ...");
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		    if(visitor.isPresent()){
		    	visitor.get().listening();
		    }
		    String message = null;
		    while ((message = reader.readLine()) != null) {
		        if(analizeMessage(message, socket, sender)){
			        streamCombiner.send(sender, message);
			        if(!sender.isRunning()){
			        	writeInSocket(socket, "You have been disconnected");
			        	break;
			        }
		        }
		    }
		} catch (IOException | StreamCombinerException e) {
			log.warn("Lost connection: "+e.getMessage());
		}
		disconnect(socket, sender);
	}
	
	private boolean analizeMessage(String message, Socket socket, Sender sender){
		log.debug("Read message: "+message);
		if(STOP_APPLICATION.equals(message)){
			writeInSocket(socket, "Finalize application");
			sender.timeout();
			stop();
			return false;
		}else if(CLOSE_CONNECTION.equals(message)){
			writeInSocket(socket, "Closing connection");
			sender.timeout();
			return false;
		}else{
			return true;
		}
	}
	
	public static InputListener createInputListener(int nSockets, int port, StreamCombiner streamCombiner){
		return createInputListener(nSockets, port, streamCombiner, Optional.empty());
	}
	
	static InputListener createInputListener(int nSockets, int port, StreamCombiner streamCombiner, Optional<Visitor> visitor){
		Objects.requireNonNull(streamCombiner);
		if(nSockets > LIMIT_N_SOCKETS){
			throw new IllegalArgumentException("The limit of inputs is "+LIMIT_N_SOCKETS);
		}else{
			return new InputListener(nSockets, port, streamCombiner, visitor);
		}
	}
	
}
