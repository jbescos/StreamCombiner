package es.tododev.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.tododev.combiner.api.Sender;
import es.tododev.combiner.api.StreamCombiner;
import es.tododev.combiner.api.exceptions.StreamCombinerException;
import es.tododev.combiner.api.exceptions.UnregisteredException;

public final class InputListener {

	private final static Logger log = LogManager.getLogger();
	private final int nSockets;
	private final int port;
	private final static int LIMIT_N_SOCKETS = 100;
	private final ExecutorService service;
	private final StreamCombiner streamCombiner;
	private final Optional<Visitor> visitor;
	private final Set<Socket> openSockets = Collections.synchronizedSet(new HashSet<>());
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
				if(openSockets.size() < nSockets){
					Socket socket = serverSocket.accept();
					log.debug("New connection accepted "+socket);
					writeInSocket(socket, "INFO: Connection accepted: "+socket);
					if(!service.isShutdown()){
						Sender sender = new Sender();
						streamCombiner.register(sender);
						service.execute(() -> handleRequest(socket, sender));
						openSockets.add(socket);
					}
				}
			}
			log.info("Finish server socket, closing all the connections");
			for(Socket socket : openSockets){
				writeInSocket(socket, "INFO: Server is off\n");
				socket.close();
			}
		}catch(IOException e){
			log.error("Error in server socket", e);
			stop();
		}
	}
	
	private synchronized void stop() {
		service.shutdown();
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
			log.info("Closing socket "+socket);
			openSockets.remove(socket);
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
			writer.write(message+"\n");
			writer.flush();
		} catch (IOException e) {
			log.error("Can not write response: "+message, e);
		}
	}
	
	private void handleRequest(Socket socket, Sender sender) {
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		    if(visitor.isPresent()){
		    	visitor.get().listening();
		    }
		    String message = null;
		    while ((message = reader.readLine()) != null && analizeMessage(message, socket, sender)) {
		    	try{
		    		streamCombiner.send(sender, message);
		    	}catch(StreamCombinerException e){
		    		log.error("Request not valid: "+e);
		    		writeInSocket(socket, "WARNING: "+e.getMessage());
		    	}
		    }
		} catch(UnregisteredException e) {
			log.error("Trying to write without being registered", e);
			writeInSocket(socket, "ERROR: "+e.getMessage());
		} catch (IOException e) {
			log.warn("Lost connection: "+e+". Cause: "+e);
		}
		disconnect(socket, sender);
	}
	
	private boolean analizeMessage(String message, Socket socket, Sender sender){
		if(STOP_APPLICATION.equals(message)){
			stop();
			return false;
		}else if(CLOSE_CONNECTION.equals(message)){
			writeInSocket(socket, "INFO: Closing connection");
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
