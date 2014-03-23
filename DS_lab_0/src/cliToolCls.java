import java.io.IOException;
import java.util.Scanner;


public class cliToolCls {

	private static final int RECEIVE_OPTION = 0; // used to parse receive option
	private static final int SEND_OPTION = 1; // used to parse send option
	private static final int OTHER_OPTION = 2; // used to parse other option
	private static final int LINE_SIZE = 80; // set the line size to 80
	
	public static void main(String[] args) throws IOException
	{
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

		/* initialize the message parser and start to listen of a child 
		 * thread */
		MessagePasser msgParser = new MessagePasser(configFilePath, procName);
		
		/* get the user input for target connection */
		Scanner scanner = new Scanner(System.in);
		System.out.print("Input the dest: ");
		String destNode = scanner.nextLine();
		
		while(true)
		{
			/* see if the user choose send or receive */
			int option = chooseOption(scanner);
			
			switch(option)
			{
				case SEND_OPTION:
				{
					/* set the dest and src header field 
					 * and send the message */
					Message sedMsg = new Message();
					sedMsg.set_source(procName);
					sedMsg.set_dest(destNode);
					
					/* get the other header field from 
					 * the user */
					fillMessagePayload(scanner, sedMsg);
					
					/* send the message */
					msgParser.send(sedMsg);
					break;
				}
				case RECEIVE_OPTION:
				{
					/* receive a message from receiver
					 * message queue and print it */
					Message revMsg = msgParser.receive();
					printALine();
					System.out.println(revMsg);
					printALine();
					break;
				}
				case OTHER_OPTION:
					continue;
			}
			
			/* if the user choose to terminate
			 * the connection terminate it */
			if(shouldExit(scanner))
			{
				break;
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
		System.out.print("Receive or Send(r/s)");
		String option = scanner.nextLine();
		if(option.equals("r"))
			return RECEIVE_OPTION;
		else if(option.equals("s"))
			return SEND_OPTION;
		else
			return OTHER_OPTION;
	}
	
	/**
	 * see if the user choose to terminate the connection
	 * @param scanner the scanner of the user input
	 * @return true if user choose to terminate the connection
	 *         false if user choose not
	 */
	private static boolean shouldExit(Scanner scanner)
	{
		System.out.print("Terminate Connection?(Y/N)");
		String isExit = scanner.nextLine();
		if(isExit.equals("Y"))
			return true;
		else
			return false; 
	}
	
	/**
	 * fill the message field
	 * @param scanner the scanner of the user inputs
	 * @param sedMsg the message to fill in
	 */
	private static void fillMessagePayload(Scanner scanner, Message sedMsg)
	{
		System.out.print("Input the kind: ");
		sedMsg.set_kind(scanner.nextLine());
		System.out.print("Input the data: ");
		sedMsg.set_data(scanner.nextLine());
	}
	
	/**
	 * print a line of 80 "*"
	 */
	private static void printALine()
	{
		for(int count = 0; count < LINE_SIZE; count++)
			System.out.print("*");
		System.out.println("");
	}
}












