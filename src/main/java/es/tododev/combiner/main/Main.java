package es.tododev.combiner.main;

import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.tododev.combiner.impl.CachedComparator;
import es.tododev.combiner.impl.Dto;
import es.tododev.combiner.impl.ElementManagerImpl;
import es.tododev.combiner.impl.StreamCombinerImpl;
import es.tododev.socket.InputListener;
import es.tododev.socket.OutputWriterImpl;

public class Main {
	
	private final static Logger log = LogManager.getLogger();
	private final static String FILE_NAME_OUTPUT = "output-"+System.currentTimeMillis()+".log";
	private final static int PORT = 25555;

	public static void main(String[] args) {
		int inputs = getInputs(args);
		log.info("Running StreamCombiner in port "+PORT+" and sending the output to "+Paths.get(FILE_NAME_OUTPUT).toAbsolutePath());
		ElementManagerImpl elementMgr = new ElementManagerImpl(new CachedComparator(1000, 100));
		OutputWriterImpl<Long, Dto> out = new OutputWriterImpl<>(FILE_NAME_OUTPUT, elementMgr);
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, 100000, out);
		InputListener listener = InputListener.createInputListener(inputs, PORT, combiner);
		listener.start();
	}
	
	private static int getInputs(String[] args){
		if(args.length != 1)
			throw new IllegalArgumentException("It is only allowed the number of inputs parameter");
		return Integer.parseInt(args[0]);
	}

}
