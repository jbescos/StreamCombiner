package es.tododev.combiner.impl;

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

import es.tododev.combiner.api.ElementManager;
import es.tododev.combiner.api.exceptions.ElementSerializerException;

public class ElementManagerImpl implements ElementManager<Long, Dto> {

	private final static Logger log = LogManager.getLogger();
	private final static JAXBContext jc;
	private final Comparator<Long> comparator;
	
	static{
		try {
			// The jabx properties is not working for me
			System.setProperty("javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
			jc = JAXBContext.newInstance(Dto.class);
			log.debug("JAXBContext: "+jc);
		} catch (JAXBException e) {
			log.error("Can not load JAXBContext" +e);
			throw new ExceptionInInitializerError(e);
		}
	}
	
	public ElementManagerImpl(Comparator<Long> comparator) {
		this.comparator = comparator;
	}
	
	@Override
	public Dto createFromString(String raw) throws ElementSerializerException {
		try(StringReader reader = new StringReader(raw)){
			Unmarshaller unmarsaller = jc.createUnmarshaller();
			StreamSource stream = new StreamSource(reader);
			return unmarsaller.unmarshal(stream, Dto.class).getValue();
		}catch(Exception e){
			throw new ElementSerializerException("Can not deserialize: "+raw, e);
		}
	}

	@Override
	public String createFromObj(Dto obj) throws ElementSerializerException {
		try(StringWriter writer = new StringWriter()){
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
			marshaller.marshal(obj, writer);
			return writer.toString();
		}catch(Exception e){
			throw new ElementSerializerException("Can not serialize: "+obj, e);
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
		return comparator;
	}

}
