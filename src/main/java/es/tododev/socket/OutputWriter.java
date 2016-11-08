package es.tododev.socket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Observable;
import java.util.Observer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OutputWriter implements Observer {

	private final static Logger log = LogManager.getLogger();
	private final String file;
	private final static String JUMP_LINE = System.getProperty("line.separator");
	
	public OutputWriter(String file){
		this.file = file;
	}
	
	@Override
	public void update(Observable o, Object arg) {
		try {
			Files.write(Paths.get(file), ((String)arg).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(Paths.get(file), JUMP_LINE.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			log.error("Can not write -> "+arg, e);
		}
	}

}
