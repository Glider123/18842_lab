import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.yaml.snakeyaml.Yaml;

public class MessagePasser {
	private static final String CONFIG_HEADER = "configuration"; // used to get configuration item
	private static final String SEND_RULES = "sendRules"; // used to get sendRules item
	private static final String RECEIVE_RULES = "receiveRules"; //used to get receiveRules item
	
	
	/* used to parse option */
	private static final int SEND_OPTION = 0; 
	private static final int RECEIVE_OPTION = 1; 
	
	
	/* used to parse the rules */
	private static final int DEFAULT = 0; 
	private static final int DROP = 1; 
	private static final int DUPLICATE = 2;  
	private static final int DELAY = 3;
	
	private static final int LINESIZE = 80; // set the line size to be 80
	LinkedHashMap<String, ArrayList<HashMap<String, String>>> map; // used to parse the configuration file
	
	/* used to hold input and output message buffer */
	Queue<TimeStampedMessage> senderMsgQueue;
	Queue<TimeStampedMessage> receiverMsgQueue;
	Queue<TimeStampedMessage> receiverDelayMsgQueue;
	
	
	private Socket cSocket; // used for sending socket
	
	/* used for sending stream */
	private ObjectOutputStream cOutStream;
	private ObjectInputStream cInStream;
	
	
	private boolean isFirstSetup; // see if this is the first communication
	private String configFileName; // the path of the configuration file
	private long fileEditTime; // the file last edit time
	
	/* used for synchronization */
	private Lock sedLock;
	private Lock revLock;
	
	private static int seqNum = 0; // set the initial sequence number to be 1
	
	//private ClockService clkService;
	private int procIndex;
	
	/**
	 * Used for delivering the input message
	 *
	 */
	class receiveService extends Thread
	{
		Socket socket;
		
		/**
		 * set the socket for receive
		 * @param socket the receive socket
		 */
		public receiveService(Socket socket)
		{
			this.socket = socket;
		}
		
		/**
		 * used by the start of the thread
		 */
		public void run()
		{
			ObjectInputStream sInStream = null;

			try 
			{	
				/* initialize the the in stream */
				sInStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				
				/* get the delivered message */
				TimeStampedMessage revMsg = (TimeStampedMessage)sInStream.readObject();
				
				/* check if the file is edited */
				File configFile = new File(configFileName);
				long lastEditTime = configFile.lastModified();
				
				if(lastEditTime != fileEditTime)
					parseYamlFile(configFileName);
				
				/* deliver the message */
				deliverMsg(revMsg);
			} 
			catch(EOFException e){
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
			catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					/* close the input stream */
					if(sInStream != null)
						sInStream.close();
				} catch(Exception e){e.printStackTrace();}
				try
				{
					/* close the socket */
					socket.close();
				}catch(Exception e){e.printStackTrace();}
			}
		}
		
		/**
		 * deliver the message
		 * @param revMsg the message to deliver
		 */
		private void deliverMsg(TimeStampedMessage revMsg)
		{
			/* get the state of the rule */
			int ruleState = parseRules(revMsg, RECEIVE_OPTION);
			
			
			switch(ruleState)
			{
				case DROP:
					return;
				case DELAY:
				{
					/* add it to the delay message queue*/
					revLock.lock();
					receiverDelayMsgQueue.add(new TimeStampedMessage(revMsg));
					revLock.unlock();

					//revLock.lock();
					//clkService.updateRevTimestamp(revMsg);
					//revLock.unlock();
					return;
				}
				case DUPLICATE:
				{
					/* add the duplicate message to the message queue */
					revLock.lock();
					TimeStampedMessage dupMsg = new TimeStampedMessage(revMsg);
					
					
					receiverMsgQueue.add(new TimeStampedMessage(dupMsg));
				    revLock.unlock();
				}
				default:
				{   /* add the message and clear the message delay queue */
					revLock.lock();
					receiverMsgQueue.add(new TimeStampedMessage(revMsg));
					clearReceiverMsgQueue();
					revLock.unlock();

					//revLock.lock();
					//clkService.updateRevTimestamp(revMsg);
					//revLock.unlock();
				}
			}
		}
		
		/**
		 * clear the receive delay message queue
		 */
		private void clearReceiverMsgQueue()
		{
			int qSize = receiverDelayMsgQueue.size();
			
			/* add the message in the delay queue to the message queue */
			while(qSize > 0)
			{
				TimeStampedMessage tmpMsg = receiverDelayMsgQueue.remove();
				receiverMsgQueue.add(tmpMsg);
				qSize--;
			}
		}
	}
	
	/**
	 * the thread for listening service
	 *
	 */
	class ListeningService extends Thread
	{
		int port;
		
		/**
		 * initialize the listening service
		 * @param port the port number of the listening service
		 */
		public ListeningService(int port)
		{
			this.port = port;
		}
		
		/**
		 * used for start of the thread
		 */
		public void run()
		{
			ServerSocket sServer = null;
			try 
			{
				/* initialize the server socket*/
				sServer = new ServerSocket(port);
				while(true)
				{
					/* wait for service */
					Socket socket = sServer.accept();
					
					/* fill in the receive queue*/
					fillInReceiveQueue(socket);
				}
					
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try 
				{
					/* close the service */
					sServer.close();
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * fill in the receive queue
		 * @param socket the socket for deliver message
		 * @throws InterruptedException
		 */
		private void fillInReceiveQueue(Socket socket) throws InterruptedException
		{
			receiveService revService = new receiveService(socket);
			
			/* start the thread */
			revService.start();
		}
	}
	
	
	/**
	 * Initialize the message passer
	 * @param configuration_filename the configuration file path
	 * @param local_name the process name
	 */
	public MessagePasser(String configuration_filename, String local_name)
	{
		parseYamlFile(configuration_filename);
		senderMsgQueue = new LinkedList<TimeStampedMessage>();
		receiverMsgQueue = new LinkedList<TimeStampedMessage>();
		receiverDelayMsgQueue = new LinkedList<TimeStampedMessage>();
		sedLock = new ReentrantLock();
		revLock = new ReentrantLock();

		cSocket = null;
		cOutStream = null;
		cInStream = null;
		isFirstSetup = true;
		configFileName = configuration_filename;
		
		File configFile = new File(configFileName);
		fileEditTime = configFile.lastModified();
		
		/* setup the server to listen */
		setupServer(local_name);
	}
	
	/**
	 * set up server
	 * @param local_name the name of the process
	 */
	private void setupServer(String local_name)
	{
		/* get the list of the configuration item */
		ArrayList<HashMap<String, String>> configLst = map.get(CONFIG_HEADER);
		int count = 0;
		
		/* find the port of the process */
		for(HashMap<String, String> configItem : configLst)
		{
			if(configItem.get("name").equals(local_name))
			{
				Object val = configItem.get("port");
				Integer tmp = (Integer)val;
				int port = tmp.intValue();
				
				ListeningService lService = new ListeningService(port);
				this.procIndex = count;
				lService.start();
				return;
			}
			count++;
		}
		
		/* if cannot find the node exit */
		System.err.println("cannot find the dest machine, exit...");
		System.exit(1);
	}
	

	/**
	 * set up the connection socket for the first connection
	 * @param sedMsg the message to send
	 */
	public void setupCommunication(Message sedMsg)
	{
		/* get the configuration item in the configuration file */
		ArrayList<HashMap<String, String>> configLst = map.get(CONFIG_HEADER);
		String dest = sedMsg.get_dest();
		
		boolean findName = false;
		
		/* find the ip address and port of the target node */
		for(HashMap<String, String> configItem : configLst)
		{
			if(configItem.get("name").equals(dest))
			{
				String ip = configItem.get("ip");
				Object val = configItem.get("port");
				Integer tmp = (Integer)val;
				int port = tmp.intValue();
				
				sedMsg.set_ip(ip);
				sedMsg.set_portNum(port);
				
				setupTCPConnection(ip, port);
				
				findName = true;
			}
		}
		
		/* if cannot find target node exit */
		if(!findName)
		{
			System.err.println("cannot find the dest machine, exit...");
			System.exit(1);
		}
	}
	
	/**
	 * set up tcp connection for socket
	 * @param ip the ip to connect
	 * @param port the port to connect
	 */
	private void setupTCPConnection(String ip, int port)
	{	
		try 
		{	
			/* set up connection */
			cSocket = new Socket(ip, port);
			cSocket.setKeepAlive(true);
		} 
		catch (UnknownHostException e) 
		{
			e.printStackTrace();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	/**
	 * close the socket connection
	 */
	public void closeSocket()
	{
		try
		{
			cOutStream.close();
		}catch(Exception e){}
		try
		{
			cInStream.close();
		}catch(Exception e){}
		try
		{
			cSocket.close();
		}catch(Exception e){}
	}
	
	/**
	 * send the message to the target node
	 * @param message
	 */
	void sendMsg(TimeStampedMessage tsMsg)
	{	
		try 
		{
			/* in case the socket is closed */
			if(cSocket.isClosed())
				setupCommunication(tsMsg);
			
			/* output the message to out stream */
			cOutStream = new ObjectOutputStream(new BufferedOutputStream(cSocket.getOutputStream()));	
			
			cOutStream.writeObject(tsMsg);
			cOutStream.flush();
			
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				/* close the out stream */
				cOutStream.close();
			}catch(Exception e){}
		}
	}
	
	/**
	 * set original time stamp
	 * @param tsTo to time stamped message
	 * @param tsFrom from time stamped message
	 */
	private void setOriginTSMsg(TimeStamp tsTo, TimeStamp tsFrom)
	{
		if(tsTo == null || tsFrom == null)
			return;
		tsTo.setTimeStamp(tsFrom.getType(), tsFrom.getVectorClock(), tsFrom.getLogicalClock());
	}
	
	/**
	 * send the message according to the rule
	 * @param message the message to send
	 * @throws IOException
	 */
	void send(TimeStampedMessage message) throws IOException
	{
		
		/* if is the first time to send the message set up the socket for later use */
		if(isFirstSetup)
		{
			setupCommunication(message);
			isFirstSetup = false;
		}
		
		/* check if the configuration file is edited */
		File configFile = new File(configFileName);
		long modifiedTime = configFile.lastModified();
		
		if(modifiedTime != fileEditTime)
		{
			parseYamlFile(configFileName);
			fileEditTime = modifiedTime;
		}
		
		/* increase the sequence number */
		message.set_seqNum(seqNum++);
		int ruleState = parseRules(message, SEND_OPTION);

		
		/* update clock service */
		//sedLock.lock();
		//clkService.updateTimestamp();
		TimeStampedMessage timeStampMsg = new TimeStampedMessage(message, null);
		setOriginTSMsg(message.getTimeStamp(), timeStampMsg.getTimeStamp());
		//sedLock.unlock();
		

		
		
		/* deal with the message according to the rule */
		switch(ruleState)
		{
			case DROP:
			{
				System.out.print("DROP MESSAGE");
				printALine();
				System.out.println(timeStampMsg);
				printALine();
				return;
			}
			case DELAY:
			{
				System.out.print("DELAY MESSAGE");
				printALine();
				System.out.println(timeStampMsg);
				printALine();
				senderMsgQueue.add(new TimeStampedMessage(timeStampMsg));
				return;
			}
			case DUPLICATE:
			{
				System.out.println("Duplicate Message Sent...");
				
				TimeStampedMessage dupMsg = new TimeStampedMessage(timeStampMsg);
				
				/* set the duplicate field to true */
				dupMsg.set_duplicate(true);
				sendMsg(dupMsg);
			}
			default:
			{
				sendMsg(timeStampMsg);
				
				/* clear the delay queue */
				sedLock.lock();
				clearSenderDelayQueue();
				sedLock.unlock();
			}
		}
	}
	
	/**
	 * clear the delay queue for sending message
	 * @throws IOException
	 */
	private void clearSenderDelayQueue() throws IOException
	{
		int curSize = senderMsgQueue.size();
		
		while(curSize > 0)
		{
			TimeStampedMessage tmpMsg = senderMsgQueue.remove();
			sendMsg(tmpMsg);
			curSize--;
		}
	}
	
	/**
	 * parse the rules according to the configuration file
	 * @param message the message to for parsing
	 * @param option the option to choose
	 * @return the action of the rule
	 */
	private int parseRules(Message message, int option)
	{
		ArrayList<HashMap<String, String>> rulesLst;
		if(option == SEND_OPTION)
			rulesLst = map.get(SEND_RULES);
		else
			rulesLst = map.get(RECEIVE_RULES);
		
		
		for(HashMap<String, String> item : rulesLst)
		{
			if(matchRule(item, message))
				return parseAction(item.get("action"));
		}
		return DEFAULT;
	}
	
	/**
	 * to see if the rule match message
	 * @param item the item in the configuration file
	 * @param msg the message to see
	 * @return true if a message match a rule
	 *         false if a message not match
	 */
	private boolean matchRule(HashMap<String, String> item, Message msg)
	{
		if(item.containsKey("src")&&!item.get("src").equals(msg.get_source()))
			return false;
		
		if(item.containsKey("dest")&&!item.get("dest").equals(msg.get_dest()))
			return false;
		
		if(item.containsKey("kind")&&!item.get("kind").equals(msg.get_kind()))
			return false;
		
		if(item.containsKey("seqNum"))
		{
			Object val = item.get("seqNum");
			Integer tmp = (Integer)val;
			int tmpSeqNum = tmp.intValue();
			if(tmpSeqNum != msg.get_seqNum())
				return false;
		}
		
		return true;
	}
	
	/**
	 * parse the action
	 * @param action the action to parse
	 * @return the action in integer format
	 */
	private int parseAction(String action)
	{
		if(action.equals("drop"))
			return DROP;
		else if(action.equals("duplicate"))
			return DUPLICATE;
		else if(action.equals("delay"))
			return DELAY;
		else
			return DEFAULT;
	}
	
	/**
	 * deliver a message of the message queue
	 * @return the message in the head of the queue
	 */
	public Message receive()
	{
		if(receiverMsgQueue == null || receiverMsgQueue.isEmpty())
			return null;
		else{
			/* get the first item in the receive queue */
			revLock.lock();
			Message retMsg = receiverMsgQueue.remove();	
			revLock.unlock();
			return retMsg;
		}
	}
	
	/**
	 * parse the YAML file
	 * @param configuration_filename
	 */
	@SuppressWarnings("unchecked")
	public void parseYamlFile(String configuration_filename)
	{
		Yaml yaml = new Yaml();

		/* fill the item in the configuration file in the map */
		try 
		{
			File configFile = new File(configuration_filename);
			InputStream input = new FileInputStream(configFile);
			Object obj = yaml.load(input);
			map = (LinkedHashMap<String, ArrayList<HashMap<String, String>>>)obj;
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * initialize the clock service
	 * @param clkService
	 */
	/*
	public void setClockService(ClockService clkService)
	{
		this.clkService = clkService;
	}
	*/
	
	/**
	 * get the YAML file configuration
	 * @return
	 */
	public LinkedHashMap<String, ArrayList<HashMap<String, String>>> getYamlMap()
	{
		return this.map;
	}
	
	/**
	 * get ProcInex
	 * @return
	 */
	public int getProcIndex()
	{
		return this.procIndex;
	}
	
	/**
	 * print a line of 80 "*"
	 */
	private void printALine()
	{
		for(int count = 0; count < LINESIZE; count++)
			System.out.print("*");
		System.out.println("");
	}
}
