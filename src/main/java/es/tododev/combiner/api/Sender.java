package es.tododev.combiner.api;

public class Sender {

	private boolean running = true;
	
	public void timeout(){
		running = false;
	}

	public boolean isRunning() {
		return running;
	}
	
}
