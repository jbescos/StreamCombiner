package es.tododev.socket.handler;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.tododev.combiner.Receiver;
import es.tododev.combiner.Sender;

public class InputChannelHandler implements ChannelHandler, Sender {

	private final static Logger log = LogManager.getLogger();
	private volatile boolean running = true;
	private final static String STOP = "STOP";
	private final Receiver receiver;
	
	public InputChannelHandler(Receiver receiver) {
		this.receiver = receiver;
	}

	@Override
	public void process(AsynchronousSocketChannel channel) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
		try{
			while(running){
				int bytesRead = channel.read(byteBuffer).get(20, TimeUnit.SECONDS);
				log.debug("bytes read: " + bytesRead);
				byteBuffer.flip();
				byte[] lineBytes = new byte[bytesRead];
				byteBuffer.get(lineBytes, 0, bytesRead);
				String line = new String(lineBytes);
				log.debug("Message: " + line);
				if(STOP.equals(line)){
					running = false;
				}else{
					// TODO
					receiver.send(this, line);
				}
				byteBuffer.clear();
			}
		}catch(Exception e){
			// TODO what to do?
			log.error("Unexpected exception", e);
		}
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void timeout() {
		// TODO Auto-generated method stub
		
	}

}
