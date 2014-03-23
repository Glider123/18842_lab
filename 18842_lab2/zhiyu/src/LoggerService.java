
public class LoggerService implements Runnable {

	private Logger logger;
	
	public LoggerService(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public void run() {
		System.out.println("Logger start...");
		logger.run();
		System.out.println("Logger quit...");
	}
}
