package es.tododev.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.tododev.socket.handler.ChannelHandler;

public class SocketManager implements AutoCloseable {

	private final int port;
	private final ExecutorService pool;
	private final ChannelHandler channelHandler;

	public SocketManager(int port, int threadPoolSize, ChannelHandler channelHandler) {
		this.port = port;
		this.pool = Executors.newFixedThreadPool(threadPoolSize);
		this.channelHandler = channelHandler;
	}

	public void init() throws IOException {
		AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(pool);
		final AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(group).bind(new InetSocketAddress(port));
		server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			
			@Override
			public void completed(AsynchronousSocketChannel channel, Void att) {
				server.accept(null, this);
				channelHandler.process(channel);
			}

			@Override
			public void failed(Throwable exc, Void att) {
			}
		});
	}

	@Override
	public void close() throws Exception {
		pool.shutdown();
	}

}
