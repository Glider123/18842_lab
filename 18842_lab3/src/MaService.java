import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.yaml.snakeyaml.Yaml;


public class MaService {
	
	private static final String CONFIG = "configuration";
	private static final String MAIN_GROUP = "maingroup";
	private static final String NAME = "name";
	private static final int RELEASED = 0;
	private static final int WANTED = 1;
	private static final int HELD = 2;
	private static final int NO_CRASH = 0;
	private static final String REQUEST = "req_lock";
	private static final String RELEASE = "release_lock";
	private static final String ACK = "ack";
	
	
	private String maGroup;
	private String procName;
	private int state;
	private boolean vote;
	private MulticastService mcService;
	private Lock maLock;
	private Queue<TimeStampedMessage> waitingQueue;
	private MessagePasser msgPasser;
	private boolean debug;
	
	class ReceiptService extends Thread
	{
		public void run()
		{
			while(true)
			{
				for(String grpName : mcService.getGroupMap().keySet())
				{
					TimeStampedMessage msg = mcService.multicastReceive(grpName);
					if(msg != null)
					{
						//System.out.println(msg);
						
						if(msg.get_kind().equals(REQUEST))
						{
							//System.out.println("This happened");
							if(debug)
							{
								System.out.println("*******************************REQUEST(Received)***************************");
								System.out.println(msg);
								System.out.println("***************************************************************************");
							}
							maLock.lock();
							if(state == HELD || vote == true)
								waitingQueue.add(msg);
							else
							{
								TimeStampedMessage tsMsg = new TimeStampedMessage();
								fillRespondMessage(tsMsg, msg);
								try 
								{
									msgPasser.send(tsMsg);
									if(debug)
									{
										System.out.println("*******************************ACK(SEND)***************************");
										System.out.println(tsMsg);
										System.out.println("*******************************************************************");
									}
								} 
								catch (IOException e) 
								{
									e.printStackTrace();
								}
								vote = true;
							}
							maLock.unlock();
						}
						else if(msg.get_kind().equals(RELEASE))
						{
							if(debug)
							{
								System.out.println("*******************************RELEASE(Received)***************************");
								System.out.println(msg);
								System.out.println("***************************************************************************");
							}
							maLock.lock();
							if(waitingQueue.isEmpty())
								vote = false;
							else
							{
								TimeStampedMessage ackMsg = waitingQueue.poll();
								TimeStampedMessage rMsg = new TimeStampedMessage();
								fillRespondMessage(rMsg, ackMsg);
								try 
								{
									msgPasser.send(rMsg);
									if(debug)
									{
										System.out.println("*******************************ACK(SEND)***************************");
										System.out.println(rMsg);
										System.out.println("*******************************************************************");
									}
								} catch (IOException e) 
								{
									e.printStackTrace();
								}
								vote = true;
							}
								
							maLock.unlock();
						}
					}
				}
			}
		}
		
		private void fillRespondMessage(TimeStampedMessage tsMsg, TimeStampedMessage msg)
		{
			TimeStampedMessage innerMsg = new TimeStampedMessage();
			innerMsg.set_source(procName);
			innerMsg.set_dest(msg.get_source());
			innerMsg.set_kind(ACK);
			innerMsg.setGroupName(msg.getGroupName());
			innerMsg.set_data("ack to " + msg.get_source());
			tsMsg.set_data(innerMsg);
			tsMsg.set_source(procName);
			tsMsg.set_dest(msg.get_source());
			tsMsg.set_kind(ACK);
			
		}
	}
	
	public MaService(String configFile, String procName)
	{
		initMaGroup(configFile, procName);
		this.state = RELEASED;
		this.vote = false;
		this.procName = procName;
		this.maLock = new ReentrantLock();
		
		this.mcService = new MulticastService(configFile, procName);
		this.waitingQueue = new LinkedList<TimeStampedMessage>();
		mcService.setCrashPoint(NO_CRASH);
		this.msgPasser = mcService.getMessagePasser();
		ReceiptService receiptService = new ReceiptService();
		receiptService.start();
	}
	
	public void setDebugMode(boolean debug)
	{
		this.debug = debug;
	}
	
	public void enterCriticalSection() throws IOException
	{
		state = WANTED;
		
		TimeStampedMessage sedMsg = new TimeStampedMessage();
		fillSedMsg(sedMsg);
		maLock.lock();
		mcService.multicastSend(sedMsg);
		maLock.unlock();
		if(debug)
		{
			System.out.println("*******************************REQUEST(SEND)*******************************");
			System.out.println(sedMsg);
			System.out.println("***************************************************************************");
		}
		System.out.println("Wait for entering the critical section...");
		waitForACK(sedMsg);
		maLock.lock();
		state = HELD;
		maLock.unlock();
		System.out.println("Entered critical section...");
	}
	
	private void waitForACK(TimeStampedMessage sedMsg)
	{
		TimeStampedMessage tsMsg;
		Group grp = mcService.getGroupMap().get(maGroup);
		int groupSize = grp.getSize();
		
		boolean[] ackStatus = new boolean[groupSize];
		Arrays.fill(ackStatus, false);
		
		while(!shouldExit(ackStatus))
		{
			tsMsg = mcService.multicastReceiveACK(maGroup);
			
			if(tsMsg == null)
				continue;
			
			int index = grp.getMemIndex(tsMsg.get_source());
			if(index == -1)
			{
				System.err.println("Wrong group member");
				System.exit(1);
			}
			
			if(debug)
			{
				System.out.println("*******************************ACK(Received)*******************************");
				System.out.println(tsMsg);
				System.out.println("***************************************************************************");
			}
			ackStatus[index] = true;
			
		
		
			//System.out.println(Arrays.toString(ackStatus));
		}
		//System.out.println("returned happened");
	}
	
	private boolean shouldExit(boolean[] ackStatus)
	{
		for(boolean status : ackStatus)
			if(!status)
				return false;
		return true;
	}
	
	private void fillSedMsg(TimeStampedMessage sedMsg)
	{
		sedMsg.set_source(procName);
		sedMsg.setGroupName(maGroup);
		sedMsg.set_kind("req_lock");
		sedMsg.set_data(procName + " req lock");
	}
	
	public void exitCriticalSection() throws IOException
	{
		maLock.lock();
		state = RELEASED;
		maLock.unlock();
		TimeStampedMessage tsMsg = new TimeStampedMessage();
		fillReleaseMsg(tsMsg);
		mcService.multicastSend(tsMsg);
		System.out.println("Exit critical section...");
		if(debug)
		{
			System.out.println("*******************************RELEASE(SEND)*******************************");
			System.out.println(tsMsg);
			System.out.println("***************************************************************************");
		}
	}
	
	private void fillReleaseMsg(TimeStampedMessage tsMsg)
	{
		tsMsg.set_source(procName);
		tsMsg.setGroupName(maGroup);
		tsMsg.set_kind(RELEASE);
		tsMsg.set_data(procName + " release lock");
	}
	
	
	@SuppressWarnings("unchecked")
	private void initMaGroup(String config, String procName)
	{
		Yaml yaml = new Yaml();

		/* fill the item in the configuration file in the map */
		try 
		{
			File pFile = new File(config);
			InputStream input = new FileInputStream(pFile);
			Object obj = yaml.load(input);
			LinkedHashMap<String, ArrayList<HashMap<String, String>>> map = 
					(LinkedHashMap<String, ArrayList<HashMap<String, String>>>)obj;
			
			
			for(HashMap<String, String> gMap : map.get(CONFIG))
			{
				if(gMap.get(NAME).equals(procName))
				{
					this.maGroup = gMap.get(MAIN_GROUP);
				}
			}
		} 
		catch (FileNotFoundException e) 
		{
			System.err.println("cannot find the config file");
			System.exit(1);
		}
		catch (NullPointerException e)
		{
			System.err.println("the format of yaml is wrong at least one field is empty");
			System.exit(1);
		}
	}
	
	/*
	public static void main(String[] args) throws IOException
	{
		MaService ms = new MaService("config.txt", "aaa");
		
		ms.enterCriticalSection();
		ms.exitCriticalSection();
		
		//System.out.println(new MaService("config.txt", "aaa").maGroup);
	}*/
}
