import java.util.Scanner;


public class LoggerCmdTool {
	
	private static final String PRINT_CMD = "print";
	private static final String QUIT_CMD = "quit";
	
	public static void main(String[] args) {
		// if the number of input args is not 2, exit
		if(args.length != 2) {
			System.err.println("Usage: <config_file_path> <proc_name>");
			System.exit(1);
		}
		
		// the first arg should be file path
		String configFilePath = args[0];
		
		// the second arg should be local host name
		String localHostName = args[1];

		// initiate the logger 
		Logger logger = new Logger(configFilePath, localHostName);
		Thread loggerService = new Thread(new LoggerService(logger));
		loggerService.start();
		
		// start to read user's input
		Scanner scanner = new Scanner(System.in);
		while(true)
		{
			System.out.print(">");
			String cmd = scanner.nextLine();
			
			// if user chooses to print all logged messages
			if (cmd.toLowerCase().equals(PRINT_CMD)) {
				logger.print();
				continue ;
			}
			
			// if user decides to quit
			if (cmd.toLowerCase().equals(QUIT_CMD)) {
				System.out.println("Are you sure to quit logger? y/n");
				String option = scanner.nextLine();
				if (option.equals("y")) {
					break ;
				} else if (option.equals("n")) {
					continue ;
				} else {
					while ((!option.equals("y")) && (!option.equals("n"))) {
						System.out.println("Please input 'y' or 'n':");
						option = scanner.nextLine();
					}
				}
			}
		}
		
		loggerService.interrupt();
		scanner.close();
	}
}
