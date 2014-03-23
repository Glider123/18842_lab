import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.yaml.snakeyaml.Yaml;

public class MulticastService {

	private static final String GROUP = "groups";
	private static final String NAME = "name";
	private static final String MEMBERS = "members";
	private static final int VECTOR = 1;
	
	
	private MessagePasser msgPasser; // message passer for the multicast service
	private HashMap<String, Group> groupMap; // store the group info
	private String procName; // proc Name
	private int seqNumber; // seq number
	private int crashPoint; // crash poit where it cash
	private Lock revLock; // lock
	
	
	
	
	class DeliverService extends Thread{
		public void run()
		{
			while(true)
			{
				TimeStampedMessage tsMsg = (TimeStampedMessage) msgPasser.receive();
				
				if(tsMsg != null)
				{
					TimeStampedMessage oriMsg = (TimeStampedMessage) tsMsg.get_data();
					String groupName = oriMsg.getGroupName();
					Group grp = groupMap.get(groupName);
					if(oriMsg.get_kind().equals("ack"))
					{	
						revLock.lock();
						grp.addACKQueue(oriMsg);
						revLock.unlock();
						continue;
					}
					
					
					// flooding Algorithm
					if(!grp.getReceiveLst().contains(oriMsg))
					{
						grp.getReceiveLst().add(oriMsg);
						
						if(!oriMsg.get_source().equals(procName))
						{
							// multicast
							TimeStampedMessage wrapMsg = createWrapMessage(oriMsg);
							try 
							{
								multicast(wrapMsg, groupName);
							}
							catch (IOException e) 
							{
								e.printStackTrace();
							}
						}
						// Causal ordering using vector timestamps
						revLock.lock();
						grp.getHoldBackQueue().add(oriMsg);
						deliverMessage(grp, oriMsg);
						revLock.unlock();
					}
				}
			}
		}
		
		private void deliverMessage(Group grp, TimeStampedMessage msg)
		{
			String src = msg.get_source();
			String groupName = msg.getGroupName();
			int index = getSrcIndex(src, groupName);
			if(index == -1)
			{
				grp.getReceiveQueue().add(msg);
				grp.getHoldBackQueue().remove(msg);
				System.out.println("this happens in index = -1");
				return;
			}
			if(src.equals(procName))
			{
				grp.getReceiveQueue().add(msg);
				grp.getHoldBackQueue().remove(msg);
				return;
			}
			int[] iVector = grp.getClockService().getVectorTimestamp();
			
			// used remove list to calm down concurrent modification exception
			ArrayList<TimeStampedMessage> removeLst = new ArrayList<TimeStampedMessage>();
			
					
			for(TimeStampedMessage jMsg : grp.getHoldBackQueue())
			{
				int[] jVector = jMsg.getVectorTS();
				
				if(shouldDeliver(iVector, jVector, index))
				{
					grp.getReceiveQueue().add(jMsg);
					removeLst.add(jMsg);
					
					//update timestamp
					grp.getClockService().getVectorTimestamp()[index] += 1;
				}
			}
			
			// remove the msg from the holdbackqueue
			for(TimeStampedMessage rmMsg : removeLst)
			{
				grp.getHoldBackQueue().remove(rmMsg);
			}
		}
		
		// see if it will deliver the message
		private boolean shouldDeliver(int[] iVector, int[] jVector, int index)
		{
			if(iVector == null && jVector == null)
				return true;
			
			if(iVector == null || jVector == null)
				return false;
			
			if(iVector.length != jVector.length)
				return false;
			
			for(int count = 0; count < iVector.length; count++)
			{
				if(count == index)
				{
					if(iVector[index] + 1 != jVector[index])
						return false;
				}
				else if (iVector[count] < jVector[count])
					return false;
			}
			
			return true;
		}
		
		// get the index
		private int getSrcIndex(String src, String groupName)
		{
			ArrayList<String> members = groupMap.get(groupName).getMembers();
			
			for(int count = 0; count < members.size(); count++)
			{
				if(members.get(count).equals(src))
					return count;
			}
			return -1;
		}
	}
	
	// constructor
	public MulticastService(String configFile, String procName)
	{
		this.groupMap = new HashMap<String, Group>();
		this.procName = procName;
		initialGroupMap(configFile, procName);
		this.msgPasser = new MessagePasser(configFile, procName);
		this.crashPoint = Integer.MAX_VALUE;
		DeliverService deliverWorker = new DeliverService();
		this.revLock = new ReentrantLock();
		deliverWorker.start();
	}
	
	public void setCrashPoint(int crashPoint)
	{
		this.crashPoint = crashPoint;
	}
	
	
	// api for the multicase
	public void multicastSend(TimeStampedMessage tsMsg) throws IOException
	{
		String groupName = tsMsg.getGroupName();
		
		if(!groupMap.containsKey(groupName))
		{
			System.err.println("proc " + procName + "does not include in the group " + groupName);
			System.exit(1);
		}
		
		Group targetGroup = groupMap.get(groupName);
		ClockService groupClkSvc = targetGroup.getClockService();
		groupClkSvc.updateTimestamp();
		tsMsg.set_seqNum(seqNumber++);
		
		tsMsg.getTimeStamp().setTimeStamp(VECTOR, groupClkSvc.getVectorTimestamp(), 0);
		
		TimeStampedMessage wrapMsg = createWrapMessage(tsMsg);
		multicast(wrapMsg, groupName);
	}
	
	private void multicast(TimeStampedMessage tsMsg, String groupName) throws IOException
	{
		Group targetGroup = groupMap.get(groupName);
		
		ArrayList<String> members = targetGroup.getMembers();
		int count = 0;
		
		for(String mem : members)
		{
			if(this.crashPoint < count)
				break;
			tsMsg.set_dest(mem);
			msgPasser.send(tsMsg);
		}
	}
	
	private TimeStampedMessage createWrapMessage(TimeStampedMessage tsMsg)
	{
		TimeStampedMessage wrapMessage = new TimeStampedMessage();
		wrapMessage.set_data(tsMsg);
		wrapMessage.set_source(procName);
		wrapMessage.set_kind("flooding");
		wrapMessage.setGroupName(tsMsg.getGroupName());
		wrapMessage.getTimeStamp().setTimeStamp(VECTOR, tsMsg.getVectorTS(), 0);
		return wrapMessage;
	}
	
	
	public TimeStampedMessage multicastReceiveACK(String groupName)
	{
		Queue<TimeStampedMessage> ackQueue = 
				groupMap.get(groupName).getACKQueue();
		
		revLock.lock();
		TimeStampedMessage ackMsg = ackQueue.poll();
		revLock.unlock();
		return ackMsg;
	}
	
	public HashMap<String, Group> getGroupMap()
	{
		return this.groupMap;
	}
	
	public MessagePasser getMessagePasser()
	{
		return this.msgPasser;
	}
	
	// print all the message ordered by group
	public TimeStampedMessage multicastReceive(String groupName)
	{
		PriorityQueue<TimeStampedMessage> revQueue = 
				groupMap.get(groupName).getReceiveQueue();
		
		revLock.lock();
		TimeStampedMessage tsMsg = revQueue.poll();
		revLock.unlock();
		
		return tsMsg;
		
		
		/*
		for(String groupName : groupMap.keySet())
		{
			System.out.println("*****************************" + groupName + "*****************************");
			PriorityQueue<TimeStampedMessage> revQueue = 
					groupMap.get(groupName).getReceiveQueue();
			revLock.lock();
			if(revQueue.isEmpty())
				System.out.println("No message in the group");
			while(!revQueue.isEmpty())
			{
				TimeStampedMessage tsMsg = revQueue.poll();
				System.out.println(tsMsg);
			}
			System.out.println("****************************************************************");
			revLock.unlock();
			
		}*/
	}
	
	
	@SuppressWarnings("unchecked")
	private void initialGroupMap(String configFile, String procName)
	{
		Yaml yaml = new Yaml();

		/* fill the item in the configuration file in the map */
		try 
		{
			File pFile = new File(configFile);
			InputStream input = new FileInputStream(pFile);
			Object obj = yaml.load(input);
			LinkedHashMap<String, ArrayList<HashMap<String, ArrayList<String>>>> map = 
					(LinkedHashMap<String, ArrayList<HashMap<String, ArrayList<String>>>>)obj;
			for(HashMap<String, ArrayList<String>> gMap : map.get(GROUP))
				fillInGroupMap(gMap);
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
	
	private void fillInGroupMap(HashMap<String, ArrayList<String>> map)
	{
		Object groupNameObj = (Object)map.get(NAME);
		String groupName = (String) groupNameObj;
		ArrayList<String> memLst = map.get(MEMBERS);
		Group group = new Group(map, procName);
		
		for(String member : memLst)
		{
			group.addMember(member);
			if(member.equals(procName))
			{
				groupMap.put(groupName, group);
			}
		}
		
	}
}
