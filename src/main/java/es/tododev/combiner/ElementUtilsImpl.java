package es.tododev.combiner;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Comparator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.jaxb.MarshallerProperties;

import es.tododev.combiner.dto.Dto;

public class ElementUtilsImpl implements ElementUtils<Long, Dto> {

	private final static Logger log = LogManager.getLogger();
	private final static JAXBContext jc;
	
	static{
		try {
			System.setProperty("javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
			jc = JAXBContext.newInstance(Dto.class);
			log.debug("JAXBContext: "+jc);
		} catch (JAXBException e) {
			log.error("Can not load JAXBContext", e);
			throw new ExceptionInInitializerError(e);
		}
	}
	
	@Override
	public Dto createFromString(String raw) throws JAXBException {
		Unmarshaller unmarsaller = jc.createUnmarshaller();
		try(StringReader reader = new StringReader(raw)){
			StreamSource stream = new StreamSource(reader);
			return unmarsaller.unmarshal(stream, Dto.class).getValue();
		}
	}

	@Override
	public String createFromObj(Dto obj) throws JAXBException, IOException {
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
		try(StringWriter writer = new StringWriter()){
			marshaller.marshal(obj, writer);
			return writer.toString();
		}
	}

	@Override
	public Long getID(Dto obj) {
		return obj.getTimestamp();
	}

	@Override
	public void merge(Dto original, Dto newElement) {
		original.setAmount(original.getAmount() + newElement.getAmount());
	}

	@Override
	public Comparator<Long> comparator() {
		return (x, y) -> Long.compare(x, y);
	}

}
