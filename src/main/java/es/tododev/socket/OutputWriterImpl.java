package es.tododev.socket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.tododev.combiner.api.ElementManager;
import es.tododev.combiner.api.ElementSerializerException;
import es.tododev.combiner.api.OutputException;
import es.tododev.combiner.api.OutputWriter;

public class OutputWriterImpl<ID,E> implements OutputWriter<E> {

	private final static Logger log = LogManager.getLogger();
	private final String file;
	private final ElementManager<ID,E> elementMgr;
	private final static String JUMP_LINE = System.getProperty("line.separator");
	
	public OutputWriterImpl(String file, ElementManager<ID,E> elementMgr){
		this.file = file;
		this.elementMgr = elementMgr;
	}

	@Override
	public void write(E content) throws OutputException, ElementSerializerException {
		try {
			String text = elementMgr.createFromObj(content);
			Files.write(Paths.get(file), text.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			Files.write(Paths.get(file), JUMP_LINE.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			log.error("Can not write -> "+content, e);
			throw new OutputException("Can not write -> "+content, e);
		}
		
	}

}
