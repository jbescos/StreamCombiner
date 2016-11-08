package es.tododev.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConcurrentUtils {
	
	private final static Logger log = LogManager.getLogger();

	public static <T> void run(int threads, ConcurrentTest<T> test) throws Exception{
		ExecutorService service = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch end = new CountDownLatch(threads);
		for(int i=0; i<threads; i++){
			service.execute(() -> {
				T obj = null;
				try {
					obj = test.before();
					start.await();
					test.execute(obj);
				} catch (Exception e) {
					test.onException(e);
				}finally{
					end.countDown();
					try {
						end.await();
						test.after(obj);
					} catch (Exception e) {
						log.error("Can not invoke after()", e);
					}
				}
			});
		}
		start.countDown();
		end.await();
		test.lastAction();
	}
	
	public static interface ConcurrentTest<T> {
		T before() throws Exception;
		void execute(T t) throws Exception;
		void after(T t) throws Exception;
		void onException(Exception e);
		void lastAction() throws Exception;
	}
	
}
