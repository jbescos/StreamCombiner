package es.tododev.combiner;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Combiner<T> implements Receiver {

	private final BlockingQueue<T> queue;
	private final DtoConverter<T> dtoConverter;
	
	public Combiner(DtoConverter<T> dtoConverter, int queueCapacity) {
		this.dtoConverter = dtoConverter;
		this.queue = new ArrayBlockingQueue<>(queueCapacity);
	}

	@Override
	public void send(String message) throws Exception {
		queue.put(dtoConverter.createFromString(message));
	}

}
