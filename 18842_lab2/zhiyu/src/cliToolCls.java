import java.io.IOException;
import java.util.Scanner;


public class cliToolCls {

	private static final int RECEIVE_OPTION = 0; // used to parse receive option
	private static final int SEND_OPTION = 1; // used to parse send option
	private static final int OTHER_OPTION = 2; // used to parse other option
	private static final int LINE_SIZE = 80; // set the line size to 80
	private static boolean MultiCastEnabled = true;

	private static final String CLOCK_TYPE = "clockType";

	public static void main(String[] args) throws IOException
	{
		String destNode = null;
		MulticastService MultiCastNode = null;
		int crashpoint = 0;
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
		//MessagePasser msgParser = new MessagePasser(configFilePath, procName);
		
		/* Multicast service */
		MultiCastNode = new MulticastService(configFilePath, procName);
		
		/*String clockTp = null;
		try
		{
			clockTp = msgParser.getYamlMap().get(CLOCK_TYPE).get(0).get("type");
		}
		catch(Exception e)
		{
			System.out.println("cannot find clock type");
			System.exit(1);
		}

		ClockService vectorClk;
*/
		/* get the clock type */
/*		if(clockTp.equals("logical"))
			vectorClk = new Logical(msgParser);
		else
			vectorClk = new Vector(msgParser);

		//msgParser.setClockService(vectorClk);

*/


		/* get the user input for target connection */
		Scanner scanner = new Scanner(System.in);

	//	System.out.print("Input logger name: ");
	//	String loggerName = scanner.nextLine();



		while(true)
		{		
			
			System.out.print("Enter Input: ");
			//String destNode = scanner.nextLine();

			/* see if the user choose send or receive */
			int option = chooseOption(scanner);
			//System.out.println(option);
			switch(option)
			{
			case SEND_OPTION:
			{
					System.out.println("Do you want to simulate a crash, 0 - No Crash, n -> Crash after sending nth memmber in the Group");
					crashpoint = Integer.parseInt(scanner.nextLine());
					MultiCastNode.setCrashPoint(crashpoint);
					System.out.println("Input the dest group name: ");
					destNode = scanner.nextLine(); 
				/*else {
					System.out.println("Input the dest: ");
					destNode = scanner.nextLine();
				}*/

				/* set the dest and src header field 
				 * and send the message */
				TimeStampedMessage sedMsg = new TimeStampedMessage();
				sedMsg.set_source(procName);
				sedMsg.setGroupName(destNode);

				/* get the other header field from 
				 * the user */
				System.out.println("Enter Message Details");
				fillMessagePayload(scanner, sedMsg);
				if (MultiCastEnabled) {
					MultiCastNode.multicastSend(sedMsg);
				} 
				//else {
				/* send the message */
				//	msgParser.send(sedMsg);
				//}

				// System.out.println("log the message?(y/n)");
				/*	String shouldLog = scanner.nextLine();
				if(shouldLog.equals("y"))
				{
					TimeStampedMessage logMsg = new TimeStampedMessage();
					fillInLogMsg(logMsg, sedMsg, loggerName);
					msgParser.send(logMsg);
					break;
				}	
				else
					break;
				 */
				break;
			}

			case RECEIVE_OPTION:
			{
				/* receive a message from receiver
				 * message queue and print it */
				if (MultiCastEnabled) {
					MultiCastNode.multicastReceive();
				} 
				/*else {
					Message revMsg = msgParser.receive();
					printALine();
					System.out.println(revMsg);
					printALine();
				}*/
				break;
			}
			case OTHER_OPTION:
				continue;
			}

			/* if the user choose to terminate
			 * the connection terminate it */
			if(shouldExit(scanner))
			{
				System.out.println("Exiting the system....");
				System.exit(0);
				break;
			}
		}
	}

	/**
	 * fill in the log message for the logger
	 * @param logMsg the log message
	 * @param sedMsg the send message
	 * @param logName the log node name
	 
	private static void fillInLogMsg(TimeStampedMessage logMsg, TimeStampedMessage sedMsg, String logName)
	{
		logMsg.set_data(new TimeStampedMessage(sedMsg));
		logMsg.set_dest(logName);
		logMsg.set_source(sedMsg.get_source());
		logMsg.set_kind("log");
	}
	*/

	/**
	 * choose the user option
	 * @param scanner the scanner of the user input
	 * @return the option in integer the user choose
	 */
	private static int chooseOption(Scanner scanner)
	{
		System.out.print("Receive or Send(r/s)");
		String option = scanner.nextLine();
		if(option.equals("r") || option.equals("R"))
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
		if(isExit.equals("Y") || isExit.equals("y")) {
			//System.out.println("returning true to exit");
			return true;
		}
		else {
			return false;
		}
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
	 
	private static void printALine()
	{
		for(int count = 0; count < LINE_SIZE; count++)
			System.out.print("*");
		System.out.println("");
	}
	*/
}
