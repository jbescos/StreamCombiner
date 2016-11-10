package es.tododev.combiner.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	private final static String HELP_FILE = "/help.txt";
	private final static String OUTPUT = "output.log";
	private final static int PORT = 22222;
	private final static int SOCKETS = 4;
	private final static int LENGTH = 100000;
	private final static int CACHE_SIZE = 1000;
	private final static int CACHE_CLEANUP = 100;
	private final static String SOCKETS_PARAM = "-sockets";
	private final static String PORT_PARAM = "-port";
	private final static String LENGTH_PARAM = "-length";
	private final static String OUT_FILE_PARAM = "-out_file";
	private final static String CACHE_SIZE_PARAM = "-cache_size";
	private final static String CACHE_CLEANUP_PARAM = "-cache_cleanup";

	public static void main(String[] args) throws IOException {
		printHelp();
		int inputs = getValue(args, SOCKETS_PARAM, SOCKETS, s -> Integer.parseInt(s));
		int port = getValue(args, PORT_PARAM, PORT, s -> Integer.parseInt(s));
		String output = getValue(args, OUT_FILE_PARAM, OUTPUT, s -> s);
		log.info("Running StreamCombiner in port "+port+" and sending the output to "+Paths.get(output).toAbsolutePath());
		ElementManagerImpl elementMgr = new ElementManagerImpl(new CachedComparator(getValue(args, CACHE_SIZE_PARAM, CACHE_SIZE, s -> Integer.parseInt(s)), getValue(args, CACHE_CLEANUP_PARAM, CACHE_CLEANUP, s -> Integer.parseInt(s))));
		OutputWriterImpl<Long, Dto> out = new OutputWriterImpl<>(output, elementMgr);
		StreamCombinerImpl<Long,Dto> combiner = new StreamCombinerImpl<>(elementMgr, getValue(args, LENGTH_PARAM, LENGTH, s -> Integer.parseInt(s)), out);
		InputListener listener = InputListener.createInputListener(inputs, port, combiner);
		listener.start();
	}
	
	private static <T> T getValue(String[] args, String parameter, T defaultValue, Function<String, T> function){
		for(int i=0;i<args.length-1;i++){
			if(parameter.equals(args[i])){
				return function.apply(args[i+1]);
			}
		}
		return defaultValue;
	}
	
	private static void printHelp() throws IOException{
		try(InputStream stream = Main.class.getResourceAsStream(HELP_FILE)){
			String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
			log.info(result);
		}
	}

}
