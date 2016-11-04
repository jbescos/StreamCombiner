package es.tododev.socket.handler;

import java.nio.channels.AsynchronousSocketChannel;

public interface ChannelHandler {

	void process(AsynchronousSocketChannel channel);
	
}
