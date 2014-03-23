import java.io.IOException;
import java.util.Scanner;


public class cliToolCls {

	private static final int ENTERCS_OPTION = 0; // used to parse enter option
	private static final int OTHER_OPTION = 1; // used to parse other option
	private static final int QUIT_OPTION = 2; // used to parse other option

	public static void main(String[] args) throws IOException
	{

		MaService maService = null;

		/* if the number of input args is not 2 exit */
		if(args.length != 2)
		{
			System.err.println("Usage: <config_file_path> <proc_name>");
			System.exit(1);
		}

		/* the first arg should be file path */
		String configFilePath = args[0];

		/* the second arg should be proc name */
		String procName = args[1];
		
		/* get the user input for target connection */
		Scanner scanner = new Scanner(System.in);
		
		/* ask for debug mod or not*/
		Boolean debugFlag = false;
		System.out.println("Use Debug Mode ? (y/n)");
		if (scanner.nextLine().equalsIgnoreCase("y"))
			debugFlag = true;
		
		/* Multicast service */
		maService = new MaService(configFilePath, procName);
		maService.setDebugMode(debugFlag);

		while(true)
		{		
			
			System.out.print("Enter Input: ");

			/* see if the user choose send or receive */
			int option = chooseOption(scanner);

			switch(option)
			{
			case ENTERCS_OPTION:
			{
				maService.enterCriticalSection();
				
				System.out.println("Exit critical section? (q)");
				String optionInCs = scanner.nextLine();
				while (optionInCs != null) {
					if (optionInCs.equalsIgnoreCase("exit") || optionInCs.equalsIgnoreCase("q")) {
						maService.exitCriticalSection();
						System.out.println("Exited from critical section.");
						break;
					}
					else
						optionInCs = scanner.nextLine();
				}
				break;
			}

			case OTHER_OPTION:
			{
				continue;
			}
			
		    case QUIT_OPTION:
		    {
		    	System.out.println("Exiting the system...");
		    	System.exit(0);
		    }
		
			}
		}
	}


	/**
	 * choose the user option
	 * @param scanner the scanner of the user input
	 * @return the option in integer the user choose
	 */
	private static int chooseOption(Scanner scanner)
	{
		System.out.println("Enter Critical Section(enter/e) or Quit Application(quit/q)");
		String option = scanner.nextLine();
		if(option.equalsIgnoreCase("enter") || option.equalsIgnoreCase("e"))
			return ENTERCS_OPTION;
		else if(option.equalsIgnoreCase("quit") || option.equalsIgnoreCase("q"))
			return QUIT_OPTION;
		else
			return OTHER_OPTION;
	}

}